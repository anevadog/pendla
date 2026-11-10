# Plan — Pendla

## Estado actual y siguiente paso

- **Estado:** proyecto terminado y listo para usar (2026-11-10). Fases 0 a 7 cerradas: todos los requisitos del MVP (RF-01 a RF-07, RNF-01 a RNF-03) cumplidos, 78 tests en verde en local y en GitHub Actions, y probado en Swagger contra PostgreSQL y JobTech.
- **Siguiente paso:** ninguno obligatorio. El proyecto crecerá poco a poco desde "Ideas para después". La primera candidata ya tiene diseño (Fase 8, ver abajo), pero no está implementada.
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
- Hay un solo usuario y todo funciona en local.

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

## Ideas para después

- Transporte público hasta la oferta (Trafiklab)
- Login y varios usuarios
- Frontend
- Docker y despliegue
- Varios entornos (dev/prod)
- Paginación en la búsqueda (`offset`)
- Más ciudades o búsqueda de municipios con la API de taxonomía de JobTech
- Notas y fecha de entrevista en cada candidatura (Fase 8, diseñada y sin implementar). Diseño propuesto: migración `V2` con `notes VARCHAR(2000)` e `interview_at TIMESTAMPTZ`; `interviewAt` como `Instant`; `PUT /api/applications/{id}/details`, que reemplaza los dos campos (`null` los borra); avisar en Swagger y en el README de que las notas no deben llevar datos de contacto de terceros, sin filtrarlas con expresiones regulares
- Documentar los códigos de error con `@ApiResponse` (hoy Swagger marca el 409 como "Undocumented", ver F4-1)
- Editar los campos de una candidatura
- Tests de integración contra PostgreSQL real (Testcontainers, requiere Docker)
- Paginación del listado de candidaturas (`Pageable`)
- Configurar Mockito como agente en `maven-surefire-plugin` (quita el aviso "self-attaching" de Java 25)
- Mensajes de validación en un idioma fijo (hoy dependen del idioma de la JVM)
- Bloqueo optimista con `@Version` para dos cambios de estado simultáneos sobre la misma candidatura
- Historial de cambios de estado con fecha
- Devolver el total de resultados de JobTech junto a la lista ("105 resultados, se muestran 20")
- Leer la versión de la API en Swagger desde el `pom.xml` en lugar de escribirla a mano
