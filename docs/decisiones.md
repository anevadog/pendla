# Decisiones técnicas — Pendla

Aquí explico qué elegí en cada momento, qué descarté y por qué.

## Decisiones iniciales

### D1. PostgreSQL desde el primer día

- **Elegí:** PostgreSQL 18 en local, que ya tenía instalado.
- **Descarté:** empezar con H2 y migrar a PostgreSQL después.
- **Por qué:** PostgreSQL es lo que me voy a encontrar en una empresa, y H2 no se comporta exactamente igual. Como ya lo tenía instalado, configurarlo me costó poco. Mis tests no usan base de datos, así que tampoco me hacía falta H2 para probar.

### D2. Ciudades admitidas en una lista fija

- **Elegí:** el enum `SupportedCity` con cuatro ciudades (Stockholm, Göteborg, Malmö y Uppsala), cada una con su código de municipio de JobTech. Si me piden otra ciudad, respondo 400. La ciudad es opcional (ver F3-4).
- **Descarté:** meter la ciudad en el texto libre de la búsqueda (`q="java Göteborg"`).
- **Por qué:** el filtro por municipio es exacto; el texto libre también devuelve ofertas que solo mencionan la ciudad. Cuatro ciudades me bastan para el MVP, y ampliar la lista está en "Ideas para después".

### D3. Los datos de la candidatura los manda el cliente

- **Elegí:** el `POST` recibe el título, la empresa, la ciudad, la url y, si existe, el `jobtechId`. Si ese `jobtechId` ya está guardado, respondo 409.
- **Descarté:** recibir solo el `jobtechId` y pedir el resto de datos a JobTech desde el servidor.
- **Por qué:** me ahorro una segunda llamada a JobTech, que también podría fallar, y puedo guardar candidaturas que no vienen de JobTech.

### D4. Transiciones de estado con reglas

- **Elegí:** solo se permiten `SAVED→SENT`, `SENT→INTERVIEW/REJECTED` e `INTERVIEW→OFFER/REJECTED`. `REJECTED` y `OFFER` son finales. Cualquier otra transición devuelve 409.
- **Descarté:** dejar cambiar a cualquier estado.
- **Por qué:** así funciona un proceso de selección de verdad, la regla de negocio queda en el dominio y es fácil de probar. Uso 409 y no 400 porque la petición está bien escrita: lo que falla es que choca con el estado actual de la candidatura.

### D5. Errores con `ProblemDetail`

- **Elegí:** `ProblemDetail` de Spring, que sigue el estándar RFC 9457.
- **Descarté:** crear mi propio `record ErrorResponse`.
- **Por qué:** es un estándar, ya viene con Spring y no tengo que mantener otra clase.

### D6. Idioma

- **Elegí:** código, endpoints y README en inglés; `docs/` en español.
- **Descarté:** todo en español.
- **Por qué:** el README lo van a leer reclutadores en Suecia. El plan y estas decisiones son mis notas de trabajo.

### D7. Tablas creadas por Hibernate (`ddl-auto=update`)

- **Elegí:** que Hibernate cree y actualice las tablas a partir de las entidades.
- **Descarté:** usar Flyway desde el principio.
- **Por qué:** el MVP funciona sin migraciones versionadas. Sé que `update` no sirve en producción: no deja historial de cambios y nunca borra columnas. Flyway está en "Ideas para después". (Sustituida en la Fase 6: ver F6-1.)

## Fase 0 — Esqueleto

### F0-1. Variables de entorno desde `.env`

- **Elegí:** la línea `spring.config.import=optional:file:.env[.properties]` en `application.properties`. La conexión usa `${DB_URL}`, `${DB_USERNAME}` y `${DB_PASSWORD}`.
- **Descarté:** definir las variables a mano en el IDE y en la terminal.
- **Por qué:** Spring Boot no lee `.env` por sí solo, y con esta línea funciona igual desde la terminal que desde el IDE. `[.properties]` le dice a Spring que lea el archivo como `clave=valor`, y `optional:` evita que falle la importación si el archivo no existe. Aun así, sin `.env` la aplicación no arranca porque no puede resolver `${DB_URL}`. Es lo que quiero: que falle pronto y con un error claro.

### F0-2. Sin el test `contextLoads`

- **Elegí:** borrar el test `@SpringBootTest` que genera Spring Initializr.
- **Descarté:** mantenerlo.
- **Por qué:** ese test arranca la aplicación entera y necesita PostgreSQL encendido, y no quiero que mis tests dependan de la base de datos. Que la aplicación arranca lo compruebo a mano al cerrar cada fase.

### F0-3. Un ping propio en lugar de Actuator

- **Elegí:** un `PingController` con `GET /api/ping` y su test con `@WebMvcTest`.
- **Descarté:** Spring Boot Actuator (`/actuator/health`).
- **Por qué:** era lo más sencillo para aprender `@RestController` y su test. Actuator añade una dependencia y no me deja código propio que probar.

### F0-4. springdoc-openapi 3.2.0

- **Elegí:** springdoc 3.2.0, con la versión en una propiedad del `pom.xml`.
- **Por qué:** la rama 3.x es la que funciona con Spring Boot 4. Comprobé en Maven Central que la 3.2.0 está construida sobre Spring Boot 4.1.2, la misma versión que uso. springdoc no es parte de Spring, así que el parent de Spring Boot no decide su versión y la tengo que poner yo.

## Fase 1 — Candidaturas: crear, listar, borrar

### F1-1. Id `Long` generado por PostgreSQL

- **Elegí:** `@GeneratedValue(strategy = GenerationType.IDENTITY)`.
- **Descarté:** `UUID`.
- **Por qué:** es simple y se lee bien. Un UUID tiene sentido cuando los ids se generan en varios sistemas o no se deben poder adivinar, y aquí no pasa ninguna de las dos cosas.

### F1-2. Duplicados por `jobtechId`: comprobación en el servicio y restricción `unique`

- **Elegí:** el servicio comprueba `existsByJobtechId` y lanza una excepción que acaba en 409 con un mensaje claro. Además, la columna es `unique` como red de seguridad.
- **Descarté:** fiarme solo de la restricción y capturar el error de la base de datos.
- **Por qué:** la regla de negocio se ve en el servicio y el mensaje de error es claro. Límite que conozco: si llegan dos peticiones iguales a la vez, la segunda choca con la restricción y da 500. Con un solo usuario lo acepto. (Resuelto en la Fase 5: ver F5-4.)
- **Lo que añadí al implementar:** los campos opcionales vacíos (`""` o solo espacios) se guardan como `null`. Así dos candidaturas sin `jobtechId` no chocan con la restricción `unique`.

### F1-3. Mapeo a mano de entidad a DTO

- **Elegí:** el método estático `JobApplicationResponse.from(entity)`.
- **Descarté:** MapStruct.
- **Por qué:** son 8 campos. No necesito una librería ni generación de código para eso.

### F1-4. Entidad sin setters

- **Elegí:** un constructor `protected` sin argumentos, que lo exige JPA, y un constructor público con los campos obligatorios que pone el estado `SAVED` y la fecha `createdAt`. Solo hay getters.
- **Descarté:** getters y setters para todo.
- **Por qué:** así nadie puede dejar la entidad en un estado incoherente desde fuera. El estado solo cambia con `changeStatus` (ver F2-1).

### F1-5. `@URL` para validar la url

- **Elegí:** `@URL` de Hibernate Validator.
- **Descarté:** `@Pattern` con una expresión regular.
- **Por qué:** se lee mejor y valida el formato de verdad. Dos detalles: es de Hibernate y no del estándar Jakarta, y acepta otros protocolos además de `http(s)`.

### F1-6. `spring.jpa.open-in-view=false`

- **Elegí:** desactivar Open Session in View.
- **Descarté:** dejarlo activado, que es lo que viene por defecto y además muestra un aviso al arrancar.
- **Por qué:** la sesión con la base de datos se cierra cuando termina el método del servicio. Como convierto las entidades en DTOs dentro del servicio, el controlador nunca necesita esa sesión. Con OSIV activado, una carga perezosa en el controlador lanzaría consultas sin que me diera cuenta.

### F1-7. Aviso de Mockito con Java 25

- **Elegí:** ignorarlo de momento; está en "Ideas para después".
- **Descarté:** configurar Mockito como agente en el `pom.xml`.
- **Por qué:** es solo un aviso y todo funciona. Configurarlo complica el build sin aportar nada al MVP.

### F1-8. Un solo manejador de errores que hereda de `ResponseEntityExceptionHandler`

- **Elegí:** `GlobalExceptionHandler` hereda de `ResponseEntityExceptionHandler`. Sobrescribo `handleMethodArgumentNotValid` para devolver los errores por campo de un cuerpo JSON y, desde la Fase 3, también `handleHandlerMethodValidationException` para los errores de los parámetros de la URL (ver F3-5).
- **Descarté:** activar `spring.mvc.problemdetails.enabled`, que era mi primera idea, o poner `@Order(Ordered.HIGHEST_PRECEDENCE)` en mi clase.
- **Por qué:** al implementar, los tests me demostraron que con esa propiedad Spring Boot registra su propio manejador con `@Order(0)`. Ese manejador atendía la validación antes que el mío y respondía sin los errores por campo. Heredando, Spring Boot ya no registra el suyo. Me queda un solo manejador, que además responde con `ProblemDetail` a los errores propios de Spring MVC, como un JSON mal formado o un id que no es un número.

## Fase 2 — Cambio de estado

### F2-1. Las reglas en el enum y la entidad las aplica

- **Elegí:** `ApplicationStatus.canTransitionTo(target)` con un `switch` sin `default`. `JobApplication.changeStatus(newStatus)` lanza `InvalidStatusTransitionException` si la transición no está permitida.
- **Descarté:** un `Map<ApplicationStatus, Set<ApplicationStatus>>` en el servicio. Tampoco me servía pasar un `EnumSet` al constructor de cada constante, porque Java no deja usar constantes que se declaran más abajo: `SAVED(EnumSet.of(SENT))` no compila.
- **Por qué:** la regla vive en el dominio y la pruebo sin Spring ni Mockito. `changeStatus` es la única forma de cambiar el estado, así que nadie se puede saltar la regla, y sigue valiendo F1-4. Como el `switch` no tiene `default`, si algún día añado un estado el código no compila hasta que decida sus transiciones.

### F2-2. Se guarda sin llamar a `save()` (dirty checking)

- **Elegí:** el servicio carga la entidad con `findById` dentro de un método `@Transactional`, le cambia el estado y no llama a `save()`. Hibernate detecta el cambio y hace el `UPDATE` al terminar la transacción.
- **Descarté:** llamar a `repository.save(application)` igualmente.
- **Por qué:** así funciona JPA con una entidad gestionada, y `save()` sería redundante. El riesgo: si alguien quita `@Transactional`, el cambio no se guarda y los tests con mocks no lo detectan. Por eso lo comprobé a mano en Swagger contra PostgreSQL.

### F2-3. Pedir el mismo estado da 409

- **Elegí:** `SENT → SENT` es una transición no permitida y devuelve 409.
- **Descarté:** responder 200 sin cambiar nada (idempotente).
- **Por qué:** queda una sola regla, la tabla de transiciones, sin casos especiales. El cliente sabe en qué estado está la candidatura, así que pedir el mismo estado es un error suyo.

### F2-4. `PATCH /api/applications/{id}/status`

- **Elegí:** `PATCH` en una subruta, con el cuerpo `{"status": "SENT"}`.
- **Descarté:** `PUT /api/applications/{id}` con la candidatura completa.
- **Por qué:** solo cambia un campo. `PUT` significa reemplazar el recurso entero y obligaría a reenviar todos los datos. La subruta deja claro que solo se toca el estado.
- **Límite que conozco:** si el estado no existe en el enum (`"FOO"`), devuelvo 400 con el mensaje genérico de Spring, que no dice qué valores son válidos. (Resuelto en la Fase 5: ver F5-3.)

## Fase 3 — Búsqueda en JobTech

### F3-1. El cliente traduce los errores de JobTech

- **Elegí:** `JobTechClient` captura `RestClientException`. Si la causa es un `SocketTimeoutException`, lanza `JobTechTimeoutException` (504). En cualquier otro caso lanza `JobTechUnavailableException` (502): respuesta 4xx o 5xx, cuerpo ilegible o vacío, o sin conexión.
- **Descarté:** dejar que las excepciones de Spring lleguen al `@ControllerAdvice` y traducirlas allí.
- **Por qué:** toda la integración queda en una sola clase, y el resto de la aplicación ni sabe que existe `RestClient`. Mi regla prohíbe el try/catch en los controladores, no en un cliente. El timeout de conexión (3 s) también lanza un `SocketTimeoutException`, así que da 504: JobTech no respondió a tiempo.

### F3-2. `RestClient` creado a mano

- **Elegí:** un bean en `JobTechClientConfig` creado con `RestClient.builder()` y `SimpleClientHttpRequestFactory`. La URL base y los timeouts los leo de `application.properties` con `@Value`.
- **Descarté:** el `RestClient.Builder` que configura Spring Boot, con propiedades `spring.http.client.*`.
- **Por qué:** no añade dependencias, porque todo está en `spring-web`, y los timeouts se ven en el código. En Spring Boot 4 los clientes HTTP se reorganizaron en módulos y la configuración automática podría pedir otro starter. La URL y los timeouts no son secretos, así que van en `application.properties` y no en `.env`.

### F3-3. DTOs de JobTech solo con lo que uso

- **Elegí:** records anidados en `JobTechSearchResponse` con `@JsonIgnoreProperties(ignoreUnknown = true)`, y `@JsonProperty` para los nombres en snake_case.
- **Descarté:** leer la respuesta como un `Map` o un `JsonNode`.
- **Por qué:** con tipos, los errores salen al compilar y se ve claro qué leo. Si JobTech añade campos nuevos, se ignoran. Los campos de contacto (`application_details`, `application_contacts`) no están en el record, así que no los puedo leer ni por error. Un test lo comprueba metiendo un `email` en el JSON.

### F3-4. Ciudad opcional, con el nombre en sueco

- **Elegí:** `city` es opcional; sin ella busco en toda Suecia. Acepto `Stockholm`, `Göteborg`, `Malmö` y `Uppsala` sin distinguir mayúsculas, y el error 400 dice cuáles son las válidas.
- **Descarté:** que `city` fuera obligatoria, o aceptar variantes como `Goteborg` o `Gothenburg`.
- **Por qué:** RF-02 solo exige `keyword`, y buscar sin ciudad cuesta lo mismo. Cada variante de nombre sería una regla más que explicar. Valido la ciudad antes de llamar a JobTech porque JobTech responde 200 aunque el municipio no exista.

### F3-5. Errores de los parámetros de la URL con `errors`

- **Elegí:** sobrescribir `handleHandlerMethodValidationException` para que un `keyword` vacío o demasiado largo devuelva 400 con `errors.keyword`, igual que el `POST` de candidaturas.
- **Descarté:** el 400 genérico de Spring ("Validation failure").
- **Por qué:** son unas 10 líneas y así el cliente sabe qué parámetro falló. Límite que conozco: si falta el parámetro, Spring responde "Required parameter 'keyword' is not present", sin `errors`. Las dos respuestas son `ProblemDetail`.

### F3-6. Devuelvo solo la lista de ofertas

- **Elegí:** `List<JobAdResponse>`, como pide RF-01.
- **Descarté:** un objeto con el total de resultados y la lista.
- **Por qué:** es lo mínimo que cumple el requisito. Mostrar el total está en "Ideas para después".

## Fase 4 — Cierre para el portafolio

### F4-1. Los códigos de error, en la descripción de Swagger

- **Elegí:** cada `@Operation` tiene un `summary` y una `description` de una línea con sus códigos de error, por ejemplo "400 invalid keyword or city · 502 JobTech error · 504 JobTech timeout".
- **Descarté:** un `@ApiResponse` por cada código de cada endpoint, unas 15 anotaciones.
- **Por qué:** en Swagger se lee igual con mucho menos código. Con `@ApiResponse`, los controladores tendrían más líneas de documentación que de código, y el README ya tiene la tabla de errores.

### F4-2. Ejemplos reales en Swagger

- **Elegí:** `@Schema(example = ...)` en los DTOs de entrada y `@Parameter(example = ...)` en la búsqueda, con los datos de una oferta real de JobTech. La información general de la API está en `@OpenAPIDefinition`, dentro de `OpenApiConfig`.
- **Descarté:** los valores genéricos que pone springdoc (`"string"`).
- **Por qué:** "Try it out" funciona sin escribir nada y me sirve para hacer una demo. Límite: la versión `0.0.1` está escrita a mano y no se lee del `pom.xml`.

### F4-3. README con ejemplos `curl` y límites conocidos

- **Elegí:** un flujo típico en cuatro pasos con `curl` y su respuesta, la estructura de paquetes, qué prueba cada tipo de test (sin poner el número) y una sección de límites conocidos.
- **Descarté:** peticiones HTTP en crudo y una captura de Swagger.
- **Por qué:** los `curl` se pueden copiar y pegar. Una captura se queda vieja en cuanto cambio algo y añade un archivo binario. (Las capturas las añadí en la Fase 7: ver F7-1.) Contar los límites demuestra que los conozco. Comprobé los JSON de ejemplo contra las respuestas reales, y así vi que Spring 7 ya no incluye `"type": "about:blank"`.

## Fase 5 — Huecos de la API de candidaturas y CI

### F5-1. `GET /api/applications/{id}` reutiliza lo que ya había

- **Elegí:** un método `findById` en el servicio que devuelve el DTO o lanza `JobApplicationNotFoundException`, la misma excepción que usan el cambio de estado y el borrado.
- **Descarté:** devolver un `Optional` al controlador y decidir allí entre 200 y 404.
- **Por qué:** el controlador sigue sin lógica y el 404 sale del mismo sitio que los demás.

### F5-2. Filtro por estado con un parámetro opcional

- **Elegí:** `GET /api/applications?status=SENT`, con `@RequestParam(required = false) ApplicationStatus status`. Si no viene, el servicio recibe `null` y usa la consulta sin filtro. Si viene, usa `findAllByStatusOrderByCreatedAtDesc`.
- **Descarté:** un endpoint aparte (`/api/applications/by-status/SENT`), o recibir un `Optional<ApplicationStatus>` como parámetro.
- **Por qué:** filtrar una colección con un parámetro de consulta es lo habitual en REST y no cambia el endpoint que ya existía. `Optional` como parámetro de un método está desaconsejado y complica los tests; un `null` documentado en el Javadoc me basta.
- **Detalle:** Spring convierte el texto al enum sin código mío y distingue mayúsculas, así que `?status=sent` da 400. Un `?status=` vacío se trata como si no hubiera filtro.

### F5-3. El 400 de un enum inválido dice qué valores valen

- **Elegí:** sobrescribir en `GlobalExceptionHandler` dos métodos más: `handleHttpMessageNotReadable` (el valor viene en el cuerpo JSON) y `handleTypeMismatch` (viene en la URL). Si el tipo esperado es un enum, respondo `Invalid value 'FOO' for 'status'. Allowed values: ...`. En cualquier otro caso llamo a `super` y la respuesta es la de siempre.
- **Descarté:** cambiar `status` a `String` en `UpdateStatusRequest` y convertirlo a mano en el servicio.
- **Por qué:** con `String` pierdo el tipo en el DTO y el desplegable de Swagger, y no arreglaba el filtro de F5-2. La solución del manejador sirve para cualquier enum que añada después. El coste: en el cuerpo JSON tengo que mirar la causa de la excepción, que es un `InvalidFormatException` de Jackson (en Jackson 3 el paquete es `tools.jackson`), y sacar el nombre del campo del final de su ruta.
- **Detalle:** el mensaje repite el valor que mandó el cliente. Va dentro de un JSON, así que no es un riesgo, pero es una decisión consciente.

### F5-4. La carrera del `unique` se convierte en 409 en el servicio

- **Elegí:** en `create`, usar `saveAndFlush` dentro de un `try/catch` de `DataIntegrityViolationException`. Si la candidatura tiene `jobtechId`, lanzo `DuplicateJobApplicationException` (409); si no lo tiene, relanzo la excepción original. Mantengo la comprobación previa con `existsByJobtechId`.
- **Descarté:** un `@ExceptionHandler` de `DataIntegrityViolationException` en el `@ControllerAdvice`, o bloquear la tabla para que no haya carrera.
- **Por qué:** el manejador genérico convertiría en "duplicado" cualquier violación de integridad de cualquier tabla. En el servicio sé qué estaba insertando. Uso `saveAndFlush` porque el `INSERT` tiene que ejecutarse dentro del `try`: con `save`, Hibernate puede retrasarlo hasta el commit, que ocurre fuera del método. Mi regla prohíbe el try/catch en los controladores, no en el servicio.
- **Supuesto:** con `jobtechId`, la única restricción que puede fallar es la `unique`, porque los tamaños y los campos obligatorios ya se validan en el DTO. Si añado otra restricción a la tabla, tengo que revisar este `catch`.
- **Límite que conozco:** el test simula la excepción con Mockito. La carrera real contra PostgreSQL no está probada; haría falta un test de integración.

### F5-5. GitHub Actions solo para ejecutar los tests

- **Elegí:** un workflow que, en cada push y pull request a `master`, instala Java 25 (Temurin) y ejecuta `./mvnw -B test`, con permisos de solo lectura y caché de dependencias de Maven. El README muestra el badge.
- **Descarté:** añadir un servicio de PostgreSQL al workflow, o construir y publicar el `jar`.
- **Por qué:** mis tests no usan base de datos (ver F0-2), así que el workflow no necesita servicios ni secretos. No despliego nada, así que no hay nada que publicar. Es gratis en repositorios públicos.
- **Lo que añadí al implementar:** `mvnw` estaba en git sin permiso de ejecución, porque trabajo en Windows, y en el runner de Linux habría fallado. Lo arreglé con `git update-index --chmod=+x mvnw` en lugar de un `chmod` en el workflow, para que también funcione al clonar en Linux o Mac.

## Fase 6 — Esquema versionado con Flyway

### F6-1. Flyway con SQL escrito a mano y `ddl-auto=validate`

- **Elegí:** Flyway (`spring-boot-starter-flyway` y `flyway-database-postgresql`) con la migración `V1__create_job_application.sql`, que crea la tabla que antes creaba Hibernate. Hibernate pasa a `ddl-auto=validate`: ya no toca el esquema, solo comprueba al arrancar que coincide con las entidades. Sin baseline, porque la base de datos estaba vacía.
- **Descarté:** Liquibase, seguir con `ddl-auto=update` y `ddl-auto=none`.
- **Por qué:** cada cambio del esquema queda en un archivo versionado en git y se aplica una sola vez; Flyway apunta lo que ya ejecutó en la tabla `flyway_schema_history`. Liquibase hace lo mismo con changelogs en XML o YAML y más conceptos, y para una tabla me basta con SQL normal. Con `validate`, si la entidad y la tabla no coinciden, la aplicación no arranca, y prefiero ese fallo pronto a un error en mitad de una petición; `none` no comprobaría nada. Una migración que ya se ejecutó no se edita: cualquier cambio es un archivo nuevo (`V2__...`).
- **Detalle:** el SQL lo escribí copiando lo que generaba Hibernate y añadí un `CHECK` sobre `status` con los cinco valores del enum. Es una restricción nueva, así que revisé el supuesto de F5-4: al crear, el estado siempre es `SAVED`, por lo que ese `CHECK` no puede fallar en `create` y el `catch` sigue siendo correcto. Si añado un estado al enum, necesito una migración que cambie el `CHECK`.
- **Límite que conozco:** mis tests no usan base de datos (ver F0-2), así que la migración y la validación solo las compruebo arrancando la aplicación contra PostgreSQL.

## Fase 7 — Presentación del repositorio

### F7-1. Tres capturas de Swagger en el README

- **Elegí:** tres PNG en `docs/images/` (unos 200 KB en total): la lista de endpoints, una búsqueda real en JobTech y un 409 de transición no permitida. Los `curl` del README se quedan.
- **Descarté:** seguir sin capturas (lo que decidí en F4-3) o un GIF de la demo.
- **Por qué:** cambio F4-3 porque quien revisa un portafolio mira el README unos segundos, y una imagen enseña antes que un bloque de texto que la API funciona contra datos reales. Para que no se queden viejas tan rápido, solo capturo pantallas estables: la lista de endpoints, la forma de la respuesta y un error. Un GIF pesa varios MB y es más difícil de rehacer.
- **Cómo las rehago:** con la aplicación arrancada, abro Swagger UI y repito las tres peticiones del README. La candidatura del 409 la creo para la captura y la borro después.
- **Detalle:** Swagger marca el 409 como "Undocumented" porque los códigos de error están en la descripción y no en `@ApiResponse` (ver F4-1). Lo acepto.

### F7-2. Licencia MIT

- **Elegí:** un archivo `LICENSE` con la licencia MIT.
- **Descarté:** no poner licencia.
- **Por qué:** sin licencia, legalmente nadie puede reutilizar el código, y GitHub lo muestra como "No license". MIT es la más corta y la más habitual en proyectos personales.

## Fase 8 — Notas y fecha de entrevista

### F8-1. `PUT /api/applications/{id}/details` que reemplaza los dos campos

- **Elegí:** un `PUT` con `notes` e `interviewAt`. Lo que llega sustituye a lo que había: un campo `null` o ausente se borra, así que `{}` borra los dos.
- **Descarté:** un `PATCH` que cambie solo los campos que llegan, y meter los dos campos en el `POST` de creación.
- **Por qué:** el cuerpo es un `record`, y un `record` no distingue un campo que no viene de uno que viene con `null`: los dos llegan como `null`. Con `PATCH` no podría saber si quiero borrar la fecha o dejarla como está, salvo con `Optional` o un `JsonNode`, que complican el código. Con `PUT` la regla es una sola: se guarda lo que mando. El `POST` no cambia, porque al guardar una oferta todavía no hay notas ni entrevista.
- **Coste que acepto:** para cambiar solo las notas hay que volver a mandar la fecha, o se borra.

### F8-2. `Instant` para la fecha de la entrevista

- **Elegí:** `interviewAt` como `Instant` en la entidad y en los DTOs, y `TIMESTAMPTZ` en la tabla. La petición acepta una fecha ISO 8601 con desfase o con `Z`; la respuesta va siempre en UTC (`2026-11-20T10:00:00+01:00` vuelve como `2026-11-20T09:00:00Z`).
- **Descarté:** `OffsetDateTime` y `LocalDateTime`.
- **Por qué:** `TIMESTAMPTZ` en PostgreSQL guarda el instante, no el desfase con el que se escribió. Con `OffsetDateTime` parecería que conservo el `+01:00` y no es verdad. `LocalDateTime` no dice en qué zona es la hora, y una entrevista por videollamada puede ser desde otro país. `Instant` es lo que de verdad se guarda, y `createdAt` ya lo usa.
- **Detalle:** una fecha sin desfase (`2026-11-20T10:00:00`) da 400, porque no se puede convertir en un instante.

### F8-3. La fecha no depende del estado

- **Elegí:** se puede poner o quitar la fecha de entrevista en cualquier estado, y cambiar el estado no toca ni la fecha ni las notas.
- **Descarté:** exigir el estado `INTERVIEW` para poner la fecha, o pasar a `INTERVIEW` al ponerla.
- **Por qué:** en la práctica me dan la fecha antes de que yo cambie el estado, y una candidatura rechazada conserva la fecha de la entrevista que tuvo. Unir las dos cosas añadiría reglas y errores nuevos sin que nada lo necesite.

### F8-4. Aviso sobre datos de contacto, sin filtro

- **Elegí:** avisar en Swagger y en el README de que las notas no deben llevar nombres, emails ni teléfonos de reclutadores. La API no revisa el texto.
- **Descarté:** rechazar las notas que parezcan tener un email o un teléfono, con expresiones regulares.
- **Por qué:** un filtro así falla en los dos sentidos: bloquea texto normal (un número de referencia parece un teléfono) y deja pasar un nombre, que no se puede detectar con una expresión regular. Daría una falsa sensación de control. Las notas las escribo yo, así que la norma es mía y queda escrita.

### F8-5. Una fecha inválida da el 400 genérico

- **Elegí:** dejar el 400 que da Spring cuando no puede leer el cuerpo (`"detail": "Failed to read request"`), sin `errors.interviewAt`.
- **Descarté:** recibir la fecha como `String` y validarla a mano, o un manejador específico para este error.
- **Por qué:** el fallo ocurre al convertir el JSON, antes de la validación, así que no pasa por el código que rellena `errors`. Recibir un `String` me obligaría a convertirlo yo y a repetir lo que Jackson ya hace.
- **Límite que conozco:** el error no dice qué campo está mal. Las notas demasiado largas sí llevan `errors.notes`, porque eso es Bean Validation.

### F8-6. Migración `V2` con columnas que admiten `NULL`

- **Elegí:** `V2__add_notes_and_interview_at.sql` añade las dos columnas sin `NOT NULL` y sin valor por defecto. `V1` no se toca.
- **Por qué:** la tabla ya tenía filas, y en ellas los dos campos quedan vacíos, que es justo lo que significan. El límite de 2000 está en dos sitios, `@Size(max = 2000)` en el DTO y `VARCHAR(2000)` en la tabla: el primero da un 400 claro y el segundo es la última barrera.

## Fase 9 — Docker Compose y test de integración

### F9-1. Un solo test con base de datos real, el del repositorio

- **Elegí:** `JobApplicationRepositoryTest`, un `@DataJpaTest` contra un PostgreSQL 18 que Testcontainers arranca en Docker. Al arrancar el contexto, Flyway ejecuta las migraciones y Hibernate valida el esquema; después compruebo que una candidatura se guarda y se lee igual, la restricción `unique` de `jobtechId`, que dos `jobtechId` a `null` no chocan, y las dos consultas derivadas.
- **Descarté:** un `@SpringBootTest` que recorra la API por HTTP contra la base de datos, y H2 en memoria.
- **Por qué:** cubre justo lo que ningún otro test cubría (el límite de F6-1): que las migraciones y la entidad coinciden, y que la restricción de F1-2 existe de verdad. El test completo por HTTP repetiría lo que ya prueban los tests de controlador y de servicio, sería más lento y me obligaría a aislar JobTech. H2 no es PostgreSQL: no entiende igual `TIMESTAMPTZ` ni el SQL de mis migraciones, y un test que pasa en H2 no demuestra que funcione en PostgreSQL.
- **Detalle:** `@DataJpaTest` solo carga la parte de JPA (entidad, repositorio, Flyway) y envuelve cada test en una transacción que se deshace al final, así que los tests no se pisan los datos. Cambia en parte F0-2: sigo sin `contextLoads`, pero ya hay un test que necesita base de datos.

### F9-2. Los datos de conexión con `@DynamicPropertySource`

- **Elegí:** un método `@DynamicPropertySource` que pasa a Spring la URL, el usuario y la contraseña del contenedor, y `@AutoConfigureTestDatabase(replace = NONE)` para que `@DataJpaTest` use esa conexión y no busque una base de datos en memoria.
- **Descarté:** `@ServiceConnection`.
- **Por qué:** el contenedor recibe un puerto al azar, así que la URL solo se conoce cuando ya ha arrancado. Con `@DynamicPropertySource` se ve en el propio test qué tres propiedades se sustituyen. `@ServiceConnection` hace lo mismo con una anotación, pero necesita otra dependencia (`spring-boot-testcontainers`) y esconde el mecanismo.
- **Detalle:** estas propiedades tienen prioridad sobre `application.properties`, así que `${DB_URL}` no llega a leerse y el test funciona sin `.env`. Lo comprobé ejecutando los tests en una copia del proyecto sin `.env`, que es lo que pasa en GitHub Actions.

### F9-3. El test se salta si Docker no está arrancado

- **Elegí:** `@Testcontainers(disabledWithoutDocker = true)`.
- **Descarté:** que el test falle cuando no hay Docker.
- **Por qué:** quiero poder ejecutar `./mvnw test` en cualquier momento sin arrancar Docker; los otros 88 tests no lo necesitan. En GitHub Actions el runner ya tiene Docker y el test se ejecuta en cada push.
- **Límite que conozco:** si Docker fallara en el CI, el test se saltaría sin avisar. Se ve en el resumen de Maven (`Skipped: 5`), pero el build sale en verde.

### F9-4. Docker Compose solo para PostgreSQL

- **Elegí:** un `compose.yaml` con un servicio, PostgreSQL 18, con un volumen para que los datos sobrevivan a `docker compose down`. El usuario y la contraseña salen del mismo `.env` que usa la aplicación. La aplicación sigue arrancando con `./mvnw spring-boot:run`, fuera de Docker.
- **Descarté:** meter también la aplicación en un contenedor (con un `Dockerfile`), y dejar PostgreSQL instalado como único camino.
- **Por qué:** quien clona el repositorio levanta la base de datos con un comando, sin instalar PostgreSQL ni crear el usuario a mano. La aplicación en un contenedor no la necesito hasta que haya despliegue, y mientras desarrollo es más cómodo arrancarla desde el IDE.
- **Detalle:** el puerto publicado es el 5433, porque en mi máquina el 5432 ya lo usa el PostgreSQL instalado, y solo en `127.0.0.1`, para que la base de datos no sea visible desde la red. PostgreSQL instalado sigue siendo una opción válida y está en el README.

## Fase 10 — Registro y login con JWT

### F10-1. JWT y no sesión

- **Elegí:** autenticación sin estado. El login devuelve un JWT y cada petición lo manda en la cabecera `Authorization: Bearer <token>`. El servidor no guarda nada entre peticiones.
- **Descarté:** sesión con cookie, que es lo que Spring Security trae casi hecho.
- **Por qué:** es lo habitual en una API REST que consume otro programa, y se prueba fácil desde Swagger y `curl`. La sesión guarda estado en el servidor y usa cookies, y con cookies hay que protegerse de CSRF.
- **Coste que acepto:** un token no se puede anular. Si me lo roban, vale hasta que caduca. Por eso caduca en una hora, y no hay logout ni refresh tokens (están en "Ideas para después").

### F10-2. El token lo valida Spring Security, no un filtro mío

- **Elegí:** `spring-boot-starter-security-oauth2-resource-server`. Con `oauth2ResourceServer().jwt()` Spring lee la cabecera, comprueba la firma y la caducidad con mi `JwtDecoder`, y deja al usuario autenticado. Para crear el token uso el `JwtEncoder` de la misma librería.
- **Descarté:** la librería `jjwt` con un filtro propio (`OncePerRequestFilter`), que es lo que enseñan casi todos los tutoriales.
- **Por qué:** un filtro de seguridad escrito a mano son unas 80 líneas donde un descuido (tragarse una excepción, no comprobar la caducidad) deja pasar peticiones. Prefiero configurar código que ya está probado. El nombre "OAuth2 resource server" confunde: no uso OAuth2 ni un servidor de autorización externo, solo la parte que valida un JWT. Mi API firma y valida con la misma clave.
- **Detalle:** firma HS256 (HMAC con SHA-256), que usa una sola clave secreta para firmar y para verificar. Me vale porque la misma aplicación hace las dos cosas. RS256, con clave pública y privada, hace falta cuando valida un servicio distinto del que firma.

### F10-3. El secreto, en `.env` y con longitud mínima

- **Elegí:** `JWT_SECRET` en `.env`, documentado en `.env.example`. Al arrancar, si tiene menos de 32 caracteres, la aplicación falla con un mensaje claro. La caducidad (`jwt.expiration=1h`) no es secreta y va en `application.properties`.
- **Por qué:** quien conoce el secreto puede fabricar tokens válidos de cualquier usuario, así que no puede estar en el repositorio. HS256 necesita una clave de al menos 256 bits; con una más corta la librería falla más tarde y con un error menos claro.
- **Detalle:** el token está firmado, no cifrado. Cualquiera puede leer su contenido, así que solo lleva el nombre de usuario y las fechas.

### F10-4. `username` y no email

- **Elegí:** el usuario se identifica con un `username` de 3 a 30 letras, números o guiones bajos, guardado en minúsculas.
- **Descarté:** el email.
- **Por qué:** un email es un dato personal y me obligaría a pensar en verificarlo y en protegerlo cuando despliegue. No tengo recuperación de contraseña, que es para lo que serviría. Guardarlo en minúsculas evita que `Alex` y `alex` sean dos cuentas; el login hace la misma conversión.

### F10-5. Contraseñas con BCrypt y el límite de 72 bytes

- **Elegí:** guardar solo el hash BCrypt (`BCryptPasswordEncoder`). La contraseña pide de 8 a 72 caracteres, sin exigir mayúsculas ni símbolos.
- **Descarté:** SHA-256, y reglas de composición.
- **Por qué:** BCrypt añade una sal aleatoria a cada contraseña y es lento a propósito, así que dos usuarios con la misma contraseña tienen hashes distintos y probar millones de contraseñas contra una tabla robada sale caro. SHA-256 es rápido, que aquí es un defecto. Las reglas de composición no hacen más fuertes las contraseñas; la longitud sí.
- **Lo que añadí al implementar:** BCrypt solo admite 72 bytes, y `@Size` cuenta caracteres. Una contraseña de 40 `ñ` pasa la validación pero ocupa 80 bytes, y sin más habría dado un 500. El servicio comprueba los bytes y devuelve 400.

### F10-6. El login no dice si el usuario existe

- **Elegí:** el mismo 401 y el mismo mensaje ("Invalid username or password") para un usuario que no existe y para una contraseña incorrecta. El servicio compara la contraseña con `passwordEncoder.matches` y no uso el `AuthenticationManager` de Spring.
- **Descarté:** `UserDetailsService` con `AuthenticationManager`.
- **Por qué:** para un solo tipo de login son dos líneas en mi servicio, y las entiendo. `UserDetailsService` añade tres clases para llegar al mismo sitio.
- **Límite que conozco:** el registro sí revela si un usuario existe (409), y eso no se puede evitar sin un flujo por email. Además, el login responde más rápido cuando el usuario no existe, porque no calcula el hash.

### F10-7. El 401 de la seguridad sale del mismo manejador de errores

- **Elegí:** un `AuthenticationEntryPoint` de una línea que pasa la excepción al `HandlerExceptionResolver` de Spring MVC. Así llega a `GlobalExceptionHandler`, que devuelve el 401 como `ProblemDetail` con la cabecera `WWW-Authenticate: Bearer`.
- **Descarté:** escribir el JSON a mano en el `AuthenticationEntryPoint`, o dejar el 401 vacío que da Spring por defecto.
- **Por qué:** la petición sin token se rechaza en los filtros de seguridad, antes de llegar a ningún controlador, y por eso el `@ControllerAdvice` no se entera por sí solo. De esta forma todos los errores siguen saliendo del mismo sitio y con el mismo formato (D5).
- **Detalle:** el mensaje es fijo y no explica por qué se rechazó el token (caducado, firma incorrecta), para no dar pistas.

### F10-8. Qué es público y CSRF desactivado

- **Elegí:** públicos `/api/ping`, `/api/auth/**` y Swagger; todo lo demás pide token, incluida la búsqueda en JobTech. CSRF desactivado y sin sesión (`STATELESS`).
- **Por qué:** la búsqueda llama a una API externa y no quiero que alguien sin cuenta la use a través de la mía. CSRF aprovecha que el navegador manda las cookies solo; aquí no hay cookies y el token lo añade el cliente a mano, así que ese ataque no aplica.
- **Detalle:** una ruta que no existe también da 401 sin token, y no 404. Lo acepto: así no se puede averiguar qué rutas existen sin estar autenticado.
- **Límite que conozco:** las candidaturas siguen siendo comunes a todos los usuarios hasta la Fase 11, y el registro está abierto.

### F10-9. Tests de seguridad

- **Elegí:** los tests de controlador importan `SecurityConfig` y, en los endpoints protegidos, usan `@WithMockUser`. Las reglas de acceso se prueban aparte en `SecurityConfigTest` con tokens reales: sin token, válido, caducado, firmado con otra clave y texto que no es un token.
- **Por qué:** `@WebMvcTest` no carga mis clases `@Configuration`; sin el `@Import` probaría la seguridad por defecto de Spring y no la mía. `@WithMockUser` evita fabricar un token en cada uno de los 30 tests que no tratan de seguridad.
- **Detalle:** cada test fija `jwt.secret` con `@TestPropertySource`, porque en GitHub Actions no hay `.env`.

## Fase 11 — Cada candidatura pertenece a un usuario

### F11-1. La candidatura de otro usuario da 404, no 403

- **Elegí:** si pido, cambio o borro una candidatura que es de otro usuario, la respuesta es el mismo 404 y el mismo mensaje que para un id que no existe.
- **Descarté:** 403 Forbidden.
- **Por qué:** un 403 confirma que ese id existe. Los ids son consecutivos, así que cualquiera con cuenta podría recorrerlos y saber cuántas candidaturas hay en total. Además el código sale más simple: una sola consulta por id y dueño (`findByIdAndOwner`) que, si no encuentra nada, lanza la `JobApplicationNotFoundException` de siempre. No hay excepción nueva ni un caso nuevo en el `@ControllerAdvice`.
- **Detalle:** la comprobación está en un único método privado del servicio (`findOwnApplication`), que usan ver, cambiar estado, cambiar detalles y borrar.

### F11-2. El usuario llega al servicio como parámetro

- **Elegí:** cada método del controlador recibe `Authentication` (Spring lo rellena con el usuario del token) y pasa `authentication.getName()`, el username, al servicio. Todos los métodos del servicio tienen un parámetro `String username`.
- **Descarté:** leer `SecurityContextHolder` dentro del servicio, y `@AuthenticationPrincipal Jwt` en el controlador.
- **Por qué:** con el parámetro se ve en la firma que el método depende del usuario, y el test unitario del servicio solo tiene que pasar un `String`. `SecurityContextHolder` es una dependencia oculta. `@AuthenticationPrincipal Jwt` no funciona con `@WithMockUser`, y habría tenido que fabricar un token en cada test de controlador.
- **Coste que acepto:** el servicio busca al usuario por su username en cada petición, una consulta más sobre un índice único. La alternativa era meter el id del usuario en el token; ahorra la consulta, pero obliga a leer el `Jwt` en el controlador.

### F11-3. Relación `@ManyToOne` con carga `LAZY`

- **Elegí:** la entidad `JobApplication` tiene un campo `owner` de tipo `AppUser` con `@ManyToOne(fetch = FetchType.LAZY)` y `@JoinColumn(name = "user_id")`.
- **Descarté:** una columna simple `Long userId`.
- **Por qué:** en la base de datos hay una clave foránea, y `@ManyToOne` es la forma normal de decirlo en JPA. Las consultas del repositorio quedan con nombres que se leen solos (`findAllByOwnerOrderByCreatedAtDesc`). `LAZY` porque nunca necesito los datos del dueño al leer una candidatura; por defecto `@ManyToOne` es `EAGER` y traería al usuario en cada consulta.
- **Detalle:** el campo se llama `owner` y no `user`, que se lee mejor en los nombres de las consultas. `updatable = false`: una candidatura no cambia de dueño. La relación es de un solo sentido: `AppUser` no tiene una lista de candidaturas, porque no la uso. El paquete `application` pasa a depender de `user`; al revés no.

### F11-4. Migración V4: las candidaturas que ya existían

- **Elegí:** `V4__add_user_to_job_application.sql` añade `user_id` sin `NOT NULL`, asigna las filas que ya existían al usuario más antiguo (`SELECT MIN(id) FROM app_user`) y después pone el `NOT NULL`.
- **Descarté:** borrar las filas existentes, y dejar `user_id` nullable.
- **Por qué:** borrar perdía mis candidaturas reales. Con la columna nullable, la regla "toda candidatura tiene dueño" no la garantiza la base de datos, y las filas sin dueño quedarían invisibles para siempre.
- **Coste que acepto:** si hay candidaturas y ningún usuario, la migración falla y la aplicación no arranca. No se pierde nada: PostgreSQL ejecuta la migración en una transacción y Flyway la deshace entera. Solo le puede pasar a una base de datos anterior a esta fase; en una base vacía no hay filas que asignar. Probé los dos casos a mano contra la base de datos de Compose.

### F11-5. El unique de `jobtech_id` pasa a ser por usuario

- **Elegí:** V4 quita el `UNIQUE (jobtech_id)` de V1 y crea `UNIQUE (user_id, jobtech_id)`. El servicio comprueba el duplicado con `existsByOwnerAndJobtechId` y el 409 no cambia (F1-2).
- **Por qué:** dos usuarios pueden guardar la misma oferta, pero cada uno solo una vez.
- **Detalle:** el índice que crea ese unique empieza por `user_id`, así que también sirve para listar las candidaturas de un usuario y no hace falta otro. Para quitar el unique antiguo hay que saber su nombre: V1 no se lo puso, y PostgreSQL lo llamó `job_application_jobtech_id_key`. Hibernate solo valida tablas, columnas y tipos (`ddl-auto=validate`), no los unique, así que quien lo comprueba es el test de repositorio.

### F11-6. Token válido de un usuario que ya no existe

- **Elegí:** 401. El servicio lanza `UsernameNotFoundException`, de Spring Security, cuando no encuentra al usuario del token.
- **Descarté:** una excepción propia, o un 404.
- **Por qué:** el token está bien firmado y no ha caducado, pero ya no identifica a nadie, y eso es un problema de autenticación. `UsernameNotFoundException` es una `AuthenticationException`, que `GlobalExceptionHandler` ya convertía en el 401 con `WWW-Authenticate: Bearer` (F10-7): no hizo falta tocarlo.
- **Detalle:** hoy no hay forma de borrar un usuario desde la API. Puede pasar si se borra en la base de datos, o si se vacía la base de datos y se mantiene el mismo `JWT_SECRET`.

### F11-7. Tests del aislamiento entre usuarios

- **Elegí:** tres niveles. En el servicio, los mocks solo responden cuando se les pregunta por el usuario correcto, y se verifica que nunca se llama a las consultas sin dueño (`findById`, `deleteById`, `findAll`). En el repositorio, contra PostgreSQL real, dos usuarios guardan la misma oferta y cada consulta devuelve solo lo del suyo. En el controlador, `@WithMockUser(username = "alex")` y se comprueba que el servicio recibe `"alex"`; `SecurityConfigTest` comprueba lo mismo con un token real.
- **Por qué:** el fallo grave de esta fase sería usar por descuido una consulta de `JpaRepository` que no filtra por dueño. Un mock no demuestra que la consulta SQL filtra bien, y por eso esa parte está en el test con base de datos real.
- **Límite que conozco:** no hay un test automático de la API completa con dos tokens contra PostgreSQL; eso lo probé a mano. Tampoco hay test de la migración con filas ya existentes. El test de integración por HTTP sigue en "Ideas para después".

## Fase 12 — Despliegue

### F12-1. Render para la aplicación y Neon para PostgreSQL, gratis

- **Elegí:** la aplicación en Render (plan Free: 0,1 CPU y 512 MB) y la base de datos en Neon (plan Free), las dos en Frankfurt. Coste: 0 € al mes. Precios y límites consultados el 2026-11-16.
- **Descarté:**
  - Render con su propio PostgreSQL gratis: la base de datos caduca a los 30 días.
  - Railway: sin capa gratis permanente, unos 5 $ al mes.
  - Fly.io: las cuentas nuevas solo tienen una prueba corta.
  - Koyeb: las fuentes se contradecían sobre si pide tarjeta.
- **Coste que acepto:** la aplicación se duerme tras 15 minutos sin tráfico, y la primera petición después tarda alrededor de un minuto. Neon se suspende a los 5 minutos y tiene 1 GB. Para un proyecto de portafolio con un usuario me vale; en el README está avisado.
- **Detalle:** Render despliega en cada push a `master`. La región es la misma en los dos servicios para que cada consulta no cruce Europa.

### F12-2. `Dockerfile` en dos etapas

- **Elegí:** una etapa compila con el JDK 25 y `mvnw`, y la otra solo lleva el JRE 25 y el jar. La aplicación corre con un usuario sin privilegios y con `-XX:MaxRAMPercentage=75`. Un `.dockerignore` deja fuera `.env`, `target/` y `.git`.
- **Descarté:** `spring-boot:build-image` (buildpacks).
- **Por qué:** Render construye la imagen desde el repositorio, así que no necesito un registro de imágenes. Buildpacks no pide escribir nada, pero es una caja negra, y un Dockerfile de 15 líneas lo puedo explicar línea a línea. Con dos etapas, la imagen final no lleva Maven, ni el código fuente, ni el JDK.
- **Detalle:**
  - Las dependencias se descargan en un paso aparte, antes de copiar `src/`. Docker guarda cada paso en caché, así que mientras `pom.xml` no cambie no se vuelven a descargar.
  - La imagen se construye con `-DskipTests`, porque los tests ya pasan en CI y el de repositorio necesita Docker.
  - El límite de memoria del 75 % deja sitio a la JVM fuera del heap. Probado en local con un contenedor de 512 MB: usa unos 245 MB.

### F12-3. La misma configuración en local y en producción, sin perfiles

- **Elegí:** producción usa las mismas propiedades que en local (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD` y `JWT_SECRET`), definidas como variables de entorno en Render. En `application.properties` añadí dos líneas:
  - `server.port=${PORT:8080}`: Render dice en qué puerto escuchar.
  - `server.forward-headers-strategy=framework`: hace que Spring lea las cabeceras `X-Forwarded-*` del proxy.
- **Descarté:** un perfil `prod` con su propio archivo de propiedades.
- **Por qué:** no hay nada que cambie entre entornos aparte de las variables, y un perfil sería un segundo archivo que mantener sin necesidad.
- **Detalle:** Render recibe HTTPS y pasa la petición a la aplicación por HTTP. Sin `forward-headers-strategy`, Swagger generaba la URL del servidor con `http://` y el botón "Try it out" fallaba. Lo comprobé en local mandando las cabeceras a mano y después en producción.

### F12-4. El registro se cierra con un interruptor

- **Elegí:** la propiedad `registration.enabled` (variable `REGISTRATION_ENABLED`), `true` por defecto. Con `false`, `POST /api/auth/register` responde **403** como `ProblemDetail`, y es lo primero que comprueba el servicio. En producción está a `false` desde que creé mi usuario. El login sigue funcionando.
- **Descarté:**
  - Un código de invitación en el registro: un campo más, otro secreto y más casos que probar.
  - Dejar el registro abierto: cualquiera podría llenar mi base de datos y usar JobTech a través de mi API.
- **Por qué 403:** la petición se entiende y está bien formada, pero este servidor no la permite. No es un 401, porque no falta autenticación, ni un 404, porque la ruta existe y está documentada.
- **Por qué `true` por defecto:** en local y en los tests no hay que configurar nada. El precio es que en producción hay que acordarse de ponerlo a `false`.
- **Lo que pasó:** en el primer despliegue la variable no estaba en Render y el registro quedó abierto. Una comprobación con un usuario inventado lo creó; cerré el registro, borré ese usuario en Neon y comprobé que no se había registrado nadie más. La comprobación correcta es mandar un registro con un nombre que ya existe: si está abierto da 409, si está cerrado da 403, y en ningún caso crea nada.
- **Detalle:** el registro cerrado comprueba el interruptor antes de mirar la base de datos, así que la respuesta no revela si un usuario existe. La validación del cuerpo sí va antes (un cuerpo vacío da 400), porque la hace Spring antes de llamar al servicio.

### F12-5. `DB_URL` en formato JDBC

- **Lo que pasó:** el primer despliegue falló al arrancar con `'url' must start with "jdbc"`. Neon da la cadena de conexión en el formato de libpq (`postgresql://usuario:contraseña@host/base?sslmode=require`), y Spring necesita una URL JDBC.
- **Cómo queda:** `DB_URL=jdbc:postgresql://<host>/<base>?sslmode=require`, con el usuario y la contraseña en sus propias variables y la conexión directa, sin el pooler de Neon.
- **Por qué sin el pooler:** Flyway funciona mejor con una conexión directa, y con un solo usuario no necesito agrupar conexiones.

## Stack

| Decisión | Elegí | Descarté | Por qué |
|---|---|---|---|
| Versión de Java | Java 25 (LTS) | Java 21 (LTS) | Ya tenía Java 25 instalado y también es LTS. Instalar Java 21 no aportaba nada al MVP |
| Migraciones | Flyway | Liquibase | SQL normal en archivos versionados, sin XML ni YAML. Ver F6-1 |
| Build | Maven | Gradle | Es lo más habitual en ofertas junior y su configuración es declarativa (`pom.xml`) |
| Cliente HTTP | `RestClient` | WebClient, Feign | Viene con Spring y es síncrono y sencillo. WebClient es reactivo y no lo necesito; Feign añade una dependencia para una sola llamada |
| DTOs | `record` de Java | Clases con Lombok | Son inmutables y no necesitan librerías. Lombok genera código que no se ve en el fuente |
| Simular JobTech en tests | `MockRestServiceServer` | WireMock | Viene con Spring Test y me basta para el MVP |
| Autenticación | JWT validado por Spring Security | Sesión con cookie; `jjwt` con filtro propio | API sin estado y sin código de seguridad escrito a mano. Ver F10-1 y F10-2 |
| Base de datos en el test de repositorio | Testcontainers (PostgreSQL real) | H2 en memoria | H2 no se comporta igual que PostgreSQL. Ver F9-1 |
| Despliegue | Render (aplicación) + Neon (PostgreSQL), plan gratis | Railway, Fly.io, PostgreSQL de Render | 0 € al mes y sin base de datos que caduque. Ver F12-1 |
| Organización del código | Por funcionalidad (`application/`, `jobsearch/`, `common/`) | Por capas (`controller/`, `service/`…) | Todo lo de una funcionalidad está junto, y la capa se ve en el nombre de cada clase. Por capas, cada funcionalidad quedaría repartida en cinco carpetas |
