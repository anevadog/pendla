# Plan — Pendla

## Estado actual y siguiente paso

- **Etapa actual:** Etapa 2 (documentación mínima) entregada y pendiente de revisión.
- **Siguiente paso:** el autor revisa la documentación y ejecuta `git init` dentro de `pendla/` (decisión D0). Después, planificar y aprobar la Fase 0.
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
- [ ] Etapa 2: documentación mínima revisada por el autor

### Fase 0 — Esqueleto que responde

**Objetivo:** el proyecto existe, arranca y responde a una petición simple.

- [ ] Generar el proyecto en start.spring.io: Maven, Java 25, paquete `io.github.anevadog.pendla`
- [ ] Conectar con PostgreSQL local (base de datos `pendla`) leyendo la configuración del entorno
- [ ] Endpoint `GET /api/ping` → `"pong"`, con su test en MockMvc
- [ ] Swagger UI visible
- [ ] Comprobar a mano que JobTech responde (una petición GET de ejemplo) y anotar los campos reales

**Terminado cuando:** `./mvnw test` pasa, la aplicación arranca contra PostgreSQL y Swagger muestra `/api/ping`.

**Conceptos:** Maven y wrapper, `@SpringBootApplication`, `@RestController`, `application.properties`, variables de entorno y el primer test con MockMvc.

### Fase 1 — Candidaturas: crear, listar, borrar

**Objetivo:** RF-04, RF-05 y RF-07 funcionando de punta a punta.

- [ ] Entidad `JobApplication` y enum de estado
- [ ] `JobApplicationRepository` (Spring Data JPA)
- [ ] DTOs de petición y respuesta (`record`) y su mapeo
- [ ] `JobApplicationService` con la lógica, incluida la comprobación de duplicados por `jobtechId`
- [ ] `JobApplicationController` con validación de entrada
- [ ] `@ControllerAdvice` con `ProblemDetail` (400, 404, 409)
- [ ] Tests de servicio y de controlador

**Terminado cuando:** se pueden crear, listar y borrar candidaturas en Swagger, se guardan en PostgreSQL y los tests pasan.

**Conceptos:** JPA y entidades, repositorios, capas, DTOs frente a entidades, Bean Validation, manejo global de errores y Mockito.

### Fase 2 — Cambio de estado

**Objetivo:** RF-06 con las reglas de transición.

- [ ] Transiciones permitidas definidas en el enum
- [ ] `PATCH /api/applications/{id}/status`
- [ ] Excepción de transición inválida → 409
- [ ] Tests de transiciones válidas, inválidas, desde estado final e id inexistente

**Terminado cuando:** las transiciones funcionan en Swagger según la tabla del README y los tests pasan.

**Conceptos:** lógica de dominio en enums, PATCH frente a PUT, 409 frente a 400.

### Fase 3 — Búsqueda en JobTech

**Objetivo:** RF-01, RF-02 y RF-03.

- [ ] Verificar los códigos de municipio reales de las ciudades admitidas
- [ ] Cliente con `RestClient` y timeouts (connect ~3 s, read ~5 s)
- [ ] DTOs de la respuesta de JobTech, ignorando los campos desconocidos
- [ ] Mapeo a la oferta simplificada, sin datos de contacto
- [ ] `GET /api/jobs` con validación
- [ ] Errores 502 y 504 en el `@ControllerAdvice`
- [ ] Tests con `MockRestServiceServer`: éxito, error 500, respuesta ilegible y timeout

**Terminado cuando:** la búsqueda devuelve ofertas reales en Swagger, los fallos simulados devuelven 502/504 y los tests pasan.

**Conceptos:** consumo de APIs REST externas, timeouts, aislar una integración, deserialización JSON y simulación de servicios en tests.

### Fase 4 — Cierre para el portafolio

**Objetivo:** proyecto presentable y defendible.

- [ ] README completo, con ejemplos de petición y respuesta
- [ ] Revisar las descripciones de Swagger
- [ ] Repasar `docs/decisiones.md` con mis palabras
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
