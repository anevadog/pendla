# Decisiones técnicas — Pendla

Mi chuleta para la defensa: qué elegí, qué descarté y por qué.

## Etapa 1 — Análisis y propuesta

### D0. Repositorio Git propio dentro de `pendla/`

- **Elegido:** un repositorio independiente creado con `git init` dentro de la carpeta del proyecto.
- **Descartado:** usar el repositorio que ya existía en mi carpeta de usuario.
- **Motivo:** ese repositorio veía archivos personales y de configuración del sistema, y mezclaba proyectos distintos. Un repositorio por proyecto evita subir algo por error y deja un historial limpio.

### D1. PostgreSQL desde el inicio

- **Elegido:** PostgreSQL local, que ya tengo instalado (versión 18).
- **Descartado:** H2 en el MVP y migrar a PostgreSQL más adelante.
- **Motivo:** es la base de datos que se usa en empresas y no obliga a migrar después. H2 no se comporta exactamente igual que PostgreSQL. Como ya está instalado, el coste de configurarlo es bajo. Los tests de servicio y de controlador no necesitan base de datos.

### D2. Ciudad mediante un mapa fijo de códigos de municipio

- **Elegido:** un mapa con unas pocas ciudades (por ejemplo Stockholm, Göteborg, Malmö, Uppsala) traducidas a su código de municipio de JobTech. Una ciudad que no está en el mapa devuelve 400.
- **Descartado:** añadir la ciudad al texto libre de la búsqueda (`q="java Göteborg"`).
- **Motivo:** el filtro por municipio es exacto. El texto libre también devuelve ofertas que solo mencionan la ciudad. La limitación es aceptable en el MVP y ampliar la lista queda en "Ideas para después".

### D3. Los datos de la candidatura los envía el cliente

- **Elegido:** el `POST` recibe el título, la empresa, la ciudad, la url y el `jobtechId` (opcional). Un `jobtechId` repetido devuelve 409.
- **Descartado:** recibir solo el `jobtechId` y pedir los datos a JobTech desde el servidor.
- **Motivo:** evita una segunda llamada externa (y su posible fallo) y permite guardar candidaturas que no vienen de JobTech.

### D4. Transiciones de estado con reglas

- **Elegido:** transiciones permitidas en el enum: `SAVED→SENT`, `SENT→INTERVIEW/REJECTED`, `INTERVIEW→OFFER/REJECTED`. `REJECTED` y `OFFER` son finales. Una transición inválida devuelve 409.
- **Descartado:** permitir cambiar a cualquier estado.
- **Motivo:** refleja el proceso real, pone lógica de negocio en el dominio y se prueba bien. Uso 409 y no 400 porque la petición está bien formada, pero choca con el estado actual del recurso.

### D5. Errores con `ProblemDetail`

- **Elegido:** `ProblemDetail` de Spring, que sigue el estándar RFC 9457.
- **Descartado:** un `record ErrorResponse` propio.
- **Motivo:** es un estándar, viene incluido en Spring y no hay que mantener otra clase.

### D6. Idioma

- **Elegido:** código, endpoints y README en inglés. Documentación interna en español.
- **Descartado:** todo en español.
- **Motivo:** el README lo leerán reclutadores en Suecia. Las decisiones y el plan son para mí.

### D7. Esquema con `ddl-auto=update` en el MVP

- **Elegido:** Hibernate crea y actualiza las tablas (`spring.jpa.hibernate.ddl-auto=update`).
- **Descartado:** Flyway desde el inicio.
- **Motivo:** el MVP funciona sin migraciones versionadas. Sé que `update` no es adecuado en producción porque no versiona los cambios ni borra columnas. Flyway queda en "Ideas para después".

## Fase 0 — Esqueleto

### F0-1. Variables de entorno cargadas desde `.env`

- **Elegido:** `spring.config.import=optional:file:.env[.properties]` en `application.properties`. La conexión usa `${DB_URL}`, `${DB_USERNAME}` y `${DB_PASSWORD}`.
- **Descartado:** definir las variables a mano en el IDE y en la terminal.
- **Motivo:** Spring Boot no lee `.env` por sí solo. Con una línea funciona igual desde la terminal y desde el IDE. El sufijo `[.properties]` le dice a Spring que lea el archivo como `clave=valor`, y `optional:` evita que falle la importación si el archivo no existe. Aun así, sin `.env` la aplicación no arranca porque no puede resolver `${DB_URL}`, y eso es lo que queremos: fallar pronto y con un error claro.

### F0-2. Sin test `contextLoads`

- **Elegido:** borrar el test `@SpringBootTest` que genera Initializr.
- **Descartado:** mantenerlo.
- **Motivo:** ese test arranca la aplicación entera y necesita PostgreSQL encendido. Los tests no deben depender de la base de datos local. Que la aplicación arranca se comprueba a mano al cerrar cada fase.

### F0-3. Ping propio en lugar de Actuator

- **Elegido:** un `PingController` con `GET /api/ping`, probado con `@WebMvcTest`.
- **Descartado:** Spring Boot Actuator (`/actuator/health`).
- **Motivo:** es lo más sencillo para aprender `@RestController` y su test. Actuator añade una dependencia y no deja código propio que probar.

### F0-4. springdoc-openapi 3.2.0

- **Elegido:** springdoc 3.2.0, con la versión fijada en una propiedad del `pom.xml`.
- **Motivo:** la rama 3.x es la de Spring Boot 4. Comprobé en Maven Central que la 3.2.0 está construida sobre Spring Boot 4.1.2, la misma versión del proyecto. springdoc no forma parte de Spring, así que el parent de Spring Boot no gestiona su versión y hay que indicarla a mano.

## Fase 1 — Candidaturas: crear, listar, borrar

### F1-1. Id `Long` generado por PostgreSQL

- **Elegido:** `@GeneratedValue(strategy = GenerationType.IDENTITY)`.
- **Descartado:** `UUID`.
- **Motivo:** es simple y legible. UUID sirve cuando los ids se generan en varios sistemas o no deben poder adivinarse, y aquí no se da ninguno de los dos casos.

### F1-2. Duplicados por `jobtechId`: comprobación en el servicio + restricción `unique`

- **Elegido:** el servicio comprueba `existsByJobtechId` y lanza una excepción que se traduce en 409 con un mensaje claro. La columna, además, es `unique` como red de seguridad.
- **Descartado:** confiar solo en la restricción y capturar la excepción de la base de datos.
- **Motivo:** la regla de negocio se ve en el servicio y el mensaje de error es claro. Límite conocido: si llegan dos peticiones idénticas a la vez, la segunda choca con la restricción y da 500. Con un solo usuario lo acepto.
- **Añadido al implementar:** los campos opcionales vacíos (`""`, espacios) se guardan como `null`. Así dos candidaturas sin `jobtechId` no chocan con la restricción `unique`.

### F1-3. Mapeo manual entidad → DTO

- **Elegido:** el método estático `JobApplicationResponse.from(entity)`.
- **Descartado:** MapStruct.
- **Motivo:** son 8 campos; no hace falta una librería ni generación de código.

### F1-4. Entidad sin setters

- **Elegido:** un constructor `protected` sin argumentos (lo exige JPA) y un constructor público con los campos obligatorios, que fija el estado `SAVED` y `createdAt`. Solo getters.
- **Descartado:** getters y setters para todo.
- **Motivo:** la entidad no puede quedar en un estado incoherente desde fuera. El cambio de estado tendrá su propio método en la Fase 2.

### F1-5. `@URL` para validar la url

- **Elegido:** `@URL` de Hibernate Validator.
- **Descartado:** `@Pattern` con una expresión regular.
- **Motivo:** se lee mejor y valida el formato de verdad. Hay que saber que es de Hibernate, no del estándar Jakarta, y que acepta otros protocolos además de `http(s)`.

### F1-6. `spring.jpa.open-in-view=false`

- **Elegido:** desactivar Open Session in View.
- **Descartado:** dejar el valor por defecto (`true`), que además muestra un aviso al arrancar.
- **Motivo:** la sesión de base de datos se cierra al terminar el método del servicio. Como las entidades se convierten en DTOs dentro del servicio, el controlador nunca necesita la sesión. Con OSIV activo, una carga perezosa en el controlador lanzaría consultas sin que se note.

### F1-7. Aviso de Mockito con Java 25

- **Elegido:** ignorarlo por ahora; está apuntado en "Ideas para después".
- **Descartado:** configurar Mockito como agente en el `pom.xml`.
- **Motivo:** es solo un aviso y todo funciona. Configurarlo añade complejidad al build sin aportar nada al MVP.

### F1-8. Manejo de errores heredando de `ResponseEntityExceptionHandler`

- **Elegido:** `GlobalExceptionHandler` hereda de `ResponseEntityExceptionHandler` y sobrescribe `handleMethodArgumentNotValid` para devolver los errores por campo.
- **Descartado:** activar `spring.mvc.problemdetails.enabled` (lo aprobado en el diseño) o poner `@Order(Ordered.HIGHEST_PRECEDENCE)` en nuestra clase.
- **Motivo:** al implementar, los tests demostraron que con esa propiedad Spring Boot registra su propio manejador con `@Order(0)`, que atendía la validación antes que el nuestro y respondía sin los errores por campo. Heredando, Spring Boot deja de registrar el suyo: queda un solo manejador que también responde con `ProblemDetail` a los errores propios de Spring MVC (JSON mal formado, id no numérico…).

### Stack

| Decisión | Elegido | Descartado | Motivo |
|---|---|---|---|
| Versión de Java | Java 25 (LTS) | Java 21 (LTS) | Al empezar comprobé que ya tenía instalado Java 25, que también es LTS. Instalar Java 21 no aporta nada al MVP |
| Build | Maven | Gradle | Es más habitual en ofertas junior y su configuración es declarativa (`pom.xml`) |
| Cliente HTTP | `RestClient` | WebClient, Feign | Viene incluido en Spring y es síncrono y sencillo. WebClient es reactivo (no hace falta) y Feign añade una dependencia para una sola llamada |
| DTOs | `record` de Java | Clases con Lombok | Son inmutables y no necesitan librerías. Lombok oculta código que tengo que saber explicar |
| Simular JobTech en tests | `MockRestServiceServer` | WireMock | Viene incluido en Spring Test y basta para el MVP |
| Organización del código | Por funcionalidad (`application/`, `jobsearch/`, `common/`) | Por capas (`controller/`, `service/`…) | Todo lo de una funcionalidad está junto y las capas se ven en el nombre de cada clase. Por capas, cada funcionalidad se reparte en cinco carpetas |
