# Plan — Pendla

## Estado actual y siguiente paso

- **Estado:** MVP terminado (2026-11-10) y Fases 8 y 9 cerradas (2026-11-16). Fases 0 a 9 cerradas: todos los requisitos del MVP (RF-01 a RF-07, RNF-01 a RNF-03) cumplidos, más las notas y la fecha de entrevista, PostgreSQL en Docker Compose y un test de repositorio contra PostgreSQL real. 93 tests en verde en local (5 necesitan Docker), y probado contra PostgreSQL y JobTech.
- **Fase 10 cerrada (2026-11-16):** registro, login con JWT y endpoints protegidos. 117 tests en verde (5 necesitan Docker), y probado de punta a punta contra PostgreSQL y JobTech.
- **Fase 11 cerrada (2026-11-16):** cada candidatura pertenece a un usuario y la de otro usuario responde 404. 122 tests en verde (8 necesitan Docker), y probado de punta a punta con dos usuarios contra PostgreSQL.
- **Fase 12 cerrada (2026-11-16):** la API está desplegada en https://pendla.onrender.com (Render + Neon, 0 € al mes) con el registro cerrado. 125 tests en verde (8 necesitan Docker), y probado desde fuera contra la URL pública.
- **Capturas del README rehechas (2026-11-16):** las tres de `docs/images` ya muestran los endpoints de `Auth`, el botón `Authorize` y las peticiones con token.
- **Siguiente paso:** ninguno decidido.
- **Bloqueos:** ninguno.

## Requisitos del MVP

| ID | Prioridad | Requisito | Criterio de aceptación |
|---|---|---|---|
| RF-01 | Alta | Buscar ofertas por palabra clave y ciudad | `GET /api/jobs?keyword=java&city=Göteborg` → 200 con una lista de `{jobtechId, title, employer, city, url}`. Máximo 20 resultados y una sola llamada a JobTech |
| RF-02 | Alta | Validar la búsqueda | `keyword` vacío o ausente → 400. Ciudad que no está en el mapa de ciudades admitidas → 400 |
| RF-03 | Alta | Fallo de JobTech controlado | JobTech devuelve 5xx o una respuesta ilegible → 502. Tarda más del timeout (~5 s) → 504. Sin reintentos y con un error en JSON |
| RF-04 | Alta | Crear candidatura | `POST /api/applications` → 201 con estado `SAVED`. Falta el título o la empresa → 400. `jobtechId` ya guardado → 409 |
| RF-05 | Alta | Listar candidaturas | `GET /api/applications` → 200, de la más reciente a la más antigua |
| RF-06 | Alta | Cambiar estado | `PATCH /api/applications/{id}/status` → 200. Id inexistente → 404. Valor inválido → 400. Transición no permitida → 409 |
| RF-07 | Media | Borrar candidatura | `DELETE /api/applications/{id}` → 204. Id inexistente → 404 |
| RNF-01 | Alta | Errores uniformes | Todos los errores salen de un `@ControllerAdvice` con formato `ProblemDetail` |
| RNF-02 | Alta | Documentación | Swagger UI accesible en local |
| RNF-03 | Alta | Tests | Tests de servicio (Mockito), de controlador (MockMvc) y del cliente de JobTech (`MockRestServiceServer`) |

**Supuestos (comprobados en la Fase 0 y la Fase 3):**

- La JobTech JobSearch API es pública, no necesita clave y admite los parámetros `q`, `municipality` y `limit`.
- Hay un solo usuario y todo funciona en local. (Era el supuesto del MVP: desde la Fase 10 hay usuarios, y desde la Fase 11 cada uno tiene sus candidaturas.)

## Fases

### Fase 0 — Esqueleto que responde

**Objetivo:** el proyecto existe, arranca y responde a una petición simple.

- [x] Generar el proyecto en start.spring.io: Maven, Spring Boot 4.1.2, Java 25, paquete `io.github.anevadog.pendla`
- [x] Conectar con PostgreSQL local (base de datos `pendla`) leyendo la configuración del entorno
- [x] Endpoint `GET /api/ping` → `"pong"`, con su test en MockMvc
- [x] Swagger UI visible
- [x] Comprobar a mano que JobTech responde (una petición GET de ejemplo) y anotar los campos reales (ver Fase 3)

**Terminado cuando:** `./mvnw test` pasa, la aplicación arranca contra PostgreSQL y Swagger muestra `/api/ping`.

**Conceptos:** Maven y wrapper, `@SpringBootApplication`, `@RestController`, `application.properties`, variables de entorno y el primer test con MockMvc.

### Fase 1 — Candidaturas: crear, listar, borrar

**Objetivo:** RF-04, RF-05 y RF-07 funcionando de punta a punta.

- [x] Entidad `JobApplication` y enum de estado
- [x] `JobApplicationRepository` (Spring Data JPA)
- [x] DTOs de petición y respuesta (`record`) y su mapeo
- [x] `JobApplicationService` con la lógica, incluida la comprobación de duplicados por `jobtechId`
- [x] `JobApplicationController` con validación de entrada
- [x] `@ControllerAdvice` con `ProblemDetail` (400, 404, 409)
- [x] Tests de servicio y de controlador

**Terminado cuando:** se pueden crear, listar y borrar candidaturas en Swagger, se guardan en PostgreSQL y los tests pasan.

**Conceptos:** JPA y entidades, repositorios, capas, DTOs frente a entidades, Bean Validation, manejo global de errores y Mockito.

### Fase 2 — Cambio de estado

**Objetivo:** RF-06 con las reglas de transición.

- [x] Transiciones permitidas definidas en el enum
- [x] `PATCH /api/applications/{id}/status`
- [x] Excepción de transición inválida → 409
- [x] Tests de transiciones válidas, inválidas, desde estado final e id inexistente

**Terminado cuando:** las transiciones funcionan en Swagger según la tabla del README y los tests pasan.

**Conceptos:** lógica de dominio en enums, PATCH frente a PUT, 409 frente a 400, `switch` exhaustivo, dirty checking de JPA y tests parametrizados.

### Fase 3 — Búsqueda en JobTech

**Objetivo:** RF-01, RF-02 y RF-03.

**Datos reales de JobTech (comprobado el 2026-11-09 con `GET /search?q=java&limit=1`):**

- Responde 200 en ~1 s y no necesita clave.
- Raíz de la respuesta: `total.value` (número de resultados) y `hits` (lista de ofertas).
- Campos que usaremos de cada oferta:

  | Campo de JobTech | Campo nuestro | Ejemplo |
  |---|---|---|
  | `id` | `jobtechId` | `31696992` |
  | `headline` | `title` | `Integrationsutvecklare Java` |
  | `employer.name` | `employer` | `Deploja AB` |
  | `workplace_address.municipality` | `city` | `Stockholm` |
  | `webpage_url` | `url` | `https://arbetsformedlingen.se/platsbanken/annonser/31696992` |

- Para filtrar por ciudad se usa `workplace_address.municipality_concept_id` como parámetro `municipality`. Códigos verificados el 2026-11-09:

  | Ciudad | `municipality` | Código |
  |---|---|---|
  | Stockholm | `AvNB_uwa_6n6` | 0180 |
  | Göteborg | `PVZL_BQT_XtL` | 1480 |
  | Malmö | `oYPt_yRA_Smm` | 1280 |
  | Uppsala | `otaF_bQY_4ZD` | 0380 |

- Algunas ofertas tienen varios lugares de trabajo: al filtrar por Göteborg aparecen ofertas cuya dirección principal es otra ciudad (por ejemplo Alingsås). Se acepta como límite conocido.
- Un `municipality` inventado no da error: JobTech responde 200 igualmente. Por eso validamos la ciudad antes de llamar.
- `application_details` (que puede llevar un `email`) y `application_contacts` contienen datos de contacto. **No se mapean.**

- [x] Verificar los códigos de municipio reales de las ciudades admitidas
- [x] Cliente con `RestClient` y timeouts (connect ~3 s, read ~5 s)
- [x] DTOs de la respuesta de JobTech, ignorando los campos desconocidos
- [x] Mapeo a la oferta simplificada, sin datos de contacto
- [x] `GET /api/jobs` con validación
- [x] Errores 502 y 504 en el `@ControllerAdvice`
- [x] Tests con `MockRestServiceServer`: éxito, error 500, respuesta ilegible y timeout

**Terminado cuando:** la búsqueda devuelve ofertas reales en Swagger, los fallos simulados devuelven 502/504 y los tests pasan.

**Conceptos:** consumo de APIs REST externas, timeouts, aislar una integración, deserialización JSON y simulación de servicios en tests. Además: beans de `@Configuration`, `@Value` con `Duration`, enum con campos, `Optional`, validación de `@RequestParam` (method validation) y logs con SLF4J.

### Fase 4 — Cierre para el portafolio

**Objetivo:** proyecto presentable.

- [x] README completo, con ejemplos de petición y respuesta
- [x] Revisar las descripciones de Swagger
- [x] Revisar y completar `docs/decisiones.md`

**Terminado cuando:** el repositorio se entiende sin explicaciones y cada decisión está documentada.

### Fase 5 — Huecos de la API de candidaturas y CI

**Objetivo:** dejar la API de candidaturas sin fallos conocidos y ejecutar los tests en cada push.

- [x] F5-1: `GET /api/applications/{id}` → 200. Id inexistente → 404
- [x] F5-2: filtrar el listado por estado: `GET /api/applications?status=SENT`. Valor inválido → 400
- [x] F5-3: el 400 por un `status` que no existe indica los valores válidos (en el cuerpo del `PATCH` y en el filtro)
- [x] F5-4: dos peticiones a la vez con el mismo `jobtechId` → 409 en lugar de 500
- [x] F5-5: GitHub Actions que ejecuta `./mvnw test` en cada push, con badge en el README

**Terminado cuando:** los cuatro cambios funcionan en Swagger, los tests pasan en local y en GitHub Actions, y el README y las decisiones están actualizados.

**Conceptos:** parámetros de consulta opcionales, consultas derivadas con filtro, personalizar errores de deserialización, excepciones de integridad de la base de datos e integración continua.

### Fase 6 — Esquema versionado con Flyway

**Objetivo:** que el esquema de la base de datos tenga historial de cambios y no dependa de Hibernate.

- [x] F6-1: Flyway con la migración `V1__create_job_application.sql` y `ddl-auto=validate`
- [x] F6-2: README, decisiones y plan actualizados

**Terminado cuando:** la aplicación arranca contra una base de datos vacía, Flyway crea la tabla, Hibernate valida el esquema sin errores y todo funciona en Swagger como antes.

**Conceptos:** migraciones versionadas, la tabla `flyway_schema_history`, `ddl-auto=validate` frente a `update`, y restricciones `CHECK` en SQL.

### Fase 7 — Presentación del repositorio

**Objetivo:** que el repositorio se entienda y se vea bien en GitHub sin tener que arrancarlo.

- [x] F7-1: tres capturas de Swagger en el README (`docs/images/`)
- [x] F7-2: licencia MIT y la lista "What it demonstrates" actualizada con Flyway
- [x] F7-3: en GitHub, topics y repositorio fijado en el perfil

**Terminado cuando:** las imágenes se ven en la portada del repositorio en GitHub, la licencia aparece en la barra lateral y el repositorio tiene topics.

**Conceptos:** licencias de software y cómo presentar un proyecto en un portafolio.

### Fase 8 — Notas y fecha de entrevista

**Objetivo:** apuntar en cada candidatura notas libres y la fecha de la entrevista.

- [x] F8-1: migración `V2__add_notes_and_interview_at.sql` (`notes VARCHAR(2000)`, `interview_at TIMESTAMPTZ`)
- [x] F8-2: `PUT /api/applications/{id}/details` reemplaza los dos campos (`null` o ausente los borra). Notas de más de 2000 caracteres o fecha inválida → 400. Id inexistente → 404
- [x] F8-3: `notes` e `interviewAt` en todas las respuestas de candidaturas; la fecha, siempre en UTC
- [x] F8-4: README, decisiones y plan actualizados

**Terminado cuando:** el `PUT` guarda, devuelve y borra los dos campos contra PostgreSQL, y los tests pasan.

**Conceptos:** segunda migración de Flyway sobre una tabla con datos, `Instant` frente a `OffsetDateTime`, `TIMESTAMPTZ` en PostgreSQL, y `PUT` (reemplazar) frente a `PATCH` (cambiar una parte).

### Fase 9 — Docker Compose y test de integración

**Objetivo:** levantar la base de datos con un comando y comprobar las migraciones y el repositorio contra PostgreSQL real.

- [x] F9-1: `JobApplicationRepositoryTest` con `@DataJpaTest` y Testcontainers (PostgreSQL 18)
- [x] F9-2: `compose.yaml` con PostgreSQL 18 en el puerto 5433
- [x] F9-3: README, decisiones y plan actualizados

**Terminado cuando:** los tests pasan con Docker arrancado y también sin `.env`, y la aplicación arranca contra la base de datos de Compose, Flyway crea las tablas y la API responde.

**Conceptos:** contenedores e imágenes, Docker Compose, volúmenes, Testcontainers, `@DataJpaTest`, `@DynamicPropertySource` y la diferencia entre un test unitario y uno de integración.

### Fase 10 — Registro y login con JWT

**Objetivo:** que la API tenga usuarios y que solo se pueda usar con un token.

- [x] F10-1: `POST /api/auth/register` → 201. Usuario en minúsculas y contraseña guardada como hash BCrypt (migración `V3__create_app_user.sql`). Datos inválidos → 400. Usuario repetido → 409
- [x] F10-2: `POST /api/auth/login` → 200 con un JWT (HS256, caduca en 1 hora). Credenciales incorrectas → 401
- [x] F10-3: todo protegido salvo `/api/ping`, `/api/auth/**` y Swagger. Sin token o con uno inválido → 401 como `ProblemDetail`. Botón "Authorize" en Swagger
- [x] F10-4: README, decisiones y plan actualizados

**Decidido:** JWT y no sesión; validación con el soporte oficial de Spring Security y no con un filtro propio; el usuario se identifica con `username`, no con email.

**PENDIENTE:** qué hacer con el registro abierto cuando haya despliegue. Las capturas del README (`docs/images/`) son anteriores a las Fases 8 y 10 y hay que rehacerlas.

**Terminado cuando:** un usuario se registra, hace login, y con el token puede usar la API; sin token recibe 401.

**Conceptos:** hash de contraseñas con BCrypt, autenticación sin estado, estructura y firma de un JWT, y la cadena de filtros de Spring Security.

### Fase 11 — Cada candidatura pertenece a un usuario

**Objetivo:** que cada usuario vea y cambie solo sus candidaturas.

- [x] F11-1: migración `V4__add_user_to_job_application.sql`: `user_id` con clave foránea a `app_user`, las filas que ya existían pasan al usuario más antiguo, y el unique de `jobtech_id` pasa a ser por usuario
- [x] F11-2: crear, listar, ver, cambiar estado, cambiar detalles y borrar usan solo las candidaturas del usuario del token. La de otro usuario → 404, igual que un id que no existe
- [x] F11-3: dos usuarios pueden guardar la misma oferta; el mismo usuario dos veces → 409
- [x] F11-4: token válido de un usuario que ya no existe → 401
- [x] F11-5: README, decisiones y plan actualizados

**Decidido:** 404 y no 403; relación `@ManyToOne` y no una columna `Long`; el username llega al servicio como parámetro desde el controlador. Ver F11-1 a F11-7 en `docs/decisiones.md`.

**PENDIENTE:** qué hacer con el registro abierto cuando haya despliegue. Las capturas del README (`docs/images/`) son anteriores a las Fases 8 y 10 y hay que rehacerlas.

**Terminado cuando:** dos usuarios guardan la misma oferta, cada uno lista solo la suya, y ninguno puede ver, cambiar ni borrar la del otro.

**Conceptos:** relaciones `@ManyToOne`, carga `LAZY` y `EAGER`, clave foránea, unique de varias columnas, migración de una columna obligatoria en tres pasos, y la diferencia entre autenticación (quién eres) y autorización (qué puedes tocar).

### Fase 12 — Despliegue

**Objetivo:** que la API funcione en una URL pública, sin coste y sin que desconocidos puedan crearse cuenta.

- [x] F12-1: `Dockerfile` en dos etapas y `.dockerignore`. Probado en local con 512 MB contra la base de datos de Compose
- [x] F12-2: `server.port=${PORT:8080}` y `server.forward-headers-strategy=framework`, para el puerto de Render y las URLs `https://` de Swagger
- [x] F12-3: interruptor `registration.enabled`. Cerrado → `POST /api/auth/register` da 403
- [x] F12-4: Neon (PostgreSQL) y Render (aplicación) creados en Frankfurt, plan gratis. Variables de entorno en Render
- [x] F12-5: comprobado desde fuera: ping, HTTPS, Swagger, 401 sin token, 400 de validación y 403 del registro
- [x] F12-6: README, decisiones y plan actualizados

**Decidido:** Render + Neon gratis; Dockerfile propio y no buildpacks; sin perfiles; registro cerrado con un interruptor y 403. Ver F12-1 a F12-5 en `docs/decisiones.md`.

**Resuelto:** el PENDIENTE de las Fases 10 y 11 sobre el registro abierto.

**PENDIENTE:** las capturas del README (`docs/images/`) son anteriores a las Fases 8, 10, 11 y 12 y hay que rehacerlas.

**Terminado cuando:** la URL pública responde, Swagger funciona con HTTPS, el registro da 403 y mi usuario puede hacer login y usar la API.

**Conceptos:** imágenes Docker en varias etapas, la memoria de la JVM en un contenedor, variables de entorno como configuración, proxy inverso y cabeceras `X-Forwarded-*`, URL JDBC frente a cadena de conexión libpq, y los límites de un plan gratis (dormirse, arranque en frío).

## Ideas para después

- Transporte público hasta la oferta (Trafiklab)
- Refresh tokens y logout (hoy el token caduca solo, ver Fase 10)
- Frontend
- Varios entornos (dev/prod)
- Paginación en la búsqueda (`offset`)
- Más ciudades o búsqueda de municipios con la API de taxonomía de JobTech
- Indicar el campo en el 400 de una fecha inválida (`errors.interviewAt`), ver F8-5
- Documentar los códigos de error con `@ApiResponse` (hoy Swagger marca el 409 como "Undocumented", ver F4-1)
- Editar los campos de una candidatura
- Test de integración de la API completa por HTTP contra PostgreSQL (hoy solo el repositorio, ver F9-1)
- Paginación del listado de candidaturas (`Pageable`)
- Configurar Mockito como agente en `maven-surefire-plugin` (quita el aviso "self-attaching" de Java 25)
- Mensajes de validación en un idioma fijo (hoy dependen del idioma de la JVM)
- Bloqueo optimista con `@Version` para dos cambios de estado simultáneos sobre la misma candidatura
- Historial de cambios de estado con fecha
- Devolver el total de resultados de JobTech junto a la lista ("105 resultados, se muestran 20")
- Leer la versión de la API en Swagger desde el `pom.xml` en lugar de escribirla a mano
- Producción: límite de peticiones por usuario, copias de seguridad de la base de datos y dominio propio (fuera de la Fase 12)
