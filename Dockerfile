# Two stages: the first one compiles the application, the second one only runs it.
# The final image has no Maven, no source code and no JDK, so it is smaller and has less to attack.

# --- Stage 1: build the jar ---
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# First only what Maven needs to download the dependencies. Docker caches each step,
# so while pom.xml does not change, the next builds skip the download
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

# The tests already ran in CI before the deployment, and the repository test needs Docker
COPY src src
RUN ./mvnw -B package -DskipTests

# --- Stage 2: run it ---
FROM eclipse-temurin:25-jre
WORKDIR /app

# Do not run as root: if someone breaks into the application, they get a user with no privileges
RUN useradd --system --no-create-home pendla
USER pendla

COPY --from=build /app/target/*.jar app.jar

# The free instance has little memory. The JVM sizes its heap from the memory of the container:
# 75% for the heap, the rest for threads, classes and the JVM itself
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
