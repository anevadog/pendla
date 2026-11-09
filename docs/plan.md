# Plan — Pendla

## Estado actual y siguiente paso

- **Fase actual:** Fase 4 en curso. README, Swagger y `docs/decisiones.md` (reescrito en primera persona) terminados; los archivos de trabajo locales ya no se publican. 67 tests en verde.
- **Siguiente paso:** terminar el ensayo de defensa (3 rondas de preguntas, respondidas en papel) y repasar las respuestas que me hayan costado. Con eso se cierra la Fase 4 y el MVP.
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

**Supuestos (comprobar en la Fase 0 y la Fase 3):**

- La JobTech JobSearch API es pública, no necesita clave y admite los parámetros `q`, `municipality` y `limit`.
- Hay un solo usuario y todo funciona en local.

## Fases

### Etapas previas

- [x] Etapa 1: análisis y propuesta aprobados
- [x] Etapa 2: documentación mínima revisada por el autor

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

**Objetivo:** proyecto presentable y defendible.

- [x] README completo, con ejemplos de petición y respuesta
- [x] Revisar las descripciones de Swagger
- [x] Repasar `docs/decisiones.md` con mis palabras
- [ ] Ensayo de defensa: explicar cada clase sin mirar el código

**Terminado cuando:** el repositorio se entiende sin explicaciones y puedo defender cada decisión.

## Ideas para después

- Transporte público hasta la oferta (Trafiklab)
- Login y varios usuarios
- Frontend
- Docker y despliegue
- Varios entornos (dev/prod)
- Flyway para versionar el esquema de la base de datos
- Paginación en la búsqueda (`offset`)
- Más ciudades o búsqueda de municipios con la API de taxonomía de JobTech
- Filtrar el listado de candidaturas por estado
- Notas y fecha de entrevista en cada candidatura
- Editar los campos de una candidatura
- `GET /api/applications/{id}`
- Tests de integración contra PostgreSQL real (Testcontainers, requiere Docker)
- Paginación del listado de candidaturas (`Pageable`)
- Configurar Mockito como agente en `maven-surefire-plugin` (quita el aviso "self-attaching" de Java 25)
- Mensajes de validación en un idioma fijo (hoy dependen del idioma de la JVM)
- Convertir el choque con la restricción `unique` de `jobtech_id` (dos peticiones a la vez) en 409 en lugar de 500
- Mensaje de 400 que indique los valores válidos cuando `status` no existe en el enum (hoy sale el mensaje genérico de Spring)
- Bloqueo optimista con `@Version` para dos cambios de estado simultáneos sobre la misma candidatura
- Historial de cambios de estado con fecha
- Devolver el total de resultados de JobTech junto a la lista ("105 resultados, se muestran 20")
- GitHub Actions que ejecute `./mvnw test` en cada push (gratis en repositorios públicos; badge verde en el README)
- Leer la versión de la API en Swagger desde el `pom.xml` en lugar de escribirla a mano
