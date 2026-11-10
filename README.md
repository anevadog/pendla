# Pendla

[![CI](https://github.com/anevadog/pendla/actions/workflows/ci.yml/badge.svg)](https://github.com/anevadog/pendla/actions/workflows/ci.yml)

A small REST API to track my job search in Sweden.

Pendla does two things:

1. **Search real job ads** from the public JobTech API (Arbetsförmedlingen) by keyword and city, and return a simplified version of each ad (title, employer, city, link).
2. **Track my applications**: save the ads I am interested in and follow their status: `SAVED → SENT → INTERVIEW → REJECTED / OFFER`.

## Why I built it

I want to work in Sweden as a backend Java developer, and I use this tool in my own job search. It is also a portfolio project: small, finished, and every part of it explainable.

What it demonstrates:

- Layered architecture (controller, service, repository), organized by feature
- DTOs: JPA entities are never exposed in HTTP responses
- Business rules in the domain: status transitions live in the status enum
- Consuming an external API with timeouts and controlled errors (`502` / `504`, no retries)
- Input validation and centralized error handling (`@ControllerAdvice` + `ProblemDetail`)
- Persistence with Spring Data JPA and PostgreSQL
- Unit and web-layer tests that need no database and no network
- Continuous integration: GitHub Actions runs the tests on every push
- API documentation with Swagger (OpenAPI)

## Tech stack

Java 25 · Spring Boot 4.1 · Maven · Spring Data JPA · PostgreSQL · Bean Validation · RestClient · springdoc-openapi · JUnit 5 · Mockito · MockMvc · MockRestServiceServer

## Status

MVP finished and extended with a few small improvements. Technical decisions and the alternatives I discarded: [docs/decisiones.md](docs/decisiones.md) (Spanish).

## How to run

Prerequisites: Java 25 and PostgreSQL running locally.

1. Create the database user and the database (as the `postgres` superuser):

   ```sql
   CREATE USER pendla WITH PASSWORD 'your-password';
   CREATE DATABASE pendla OWNER pendla;
   ```

2. Copy `.env.example` to `.env` and set the same password. The app reads `.env` on startup.
3. Start the application (on Windows PowerShell use `.\mvnw.cmd` instead of `./mvnw`):

   ```bash
   ./mvnw spring-boot:run
   ```

4. Check it is up: `http://localhost:8080/api/ping` returns `pong`.
5. Open Swagger UI at `http://localhost:8080/swagger-ui.html`. Every endpoint has example values ready for "Try it out".

## Typical flow

The examples use `curl` in a bash shell (Git Bash on Windows). Responses are shortened.

**1. Search job ads** (max 20 results, `city` is optional):

```bash
curl "http://localhost:8080/api/jobs?keyword=java&city=Stockholm"
```

```json
[
  {
    "jobtechId": "31696992",
    "title": "Integrationsutvecklare Java",
    "employer": "Deploja AB",
    "city": "Stockholm",
    "url": "https://arbetsformedlingen.se/platsbanken/annonser/31696992"
  }
]
```

**2. Save the one I am interested in** (it starts as `SAVED`):

```bash
curl -X POST http://localhost:8080/api/applications \
  -H "Content-Type: application/json" \
  -d '{"jobtechId": "31696992", "title": "Integrationsutvecklare Java", "employer": "Deploja AB",
       "city": "Stockholm", "url": "https://arbetsformedlingen.se/platsbanken/annonser/31696992"}'
```

```json
{
  "id": 1,
  "jobtechId": "31696992",
  "title": "Integrationsutvecklare Java",
  "employer": "Deploja AB",
  "city": "Stockholm",
  "url": "https://arbetsformedlingen.se/platsbanken/annonser/31696992",
  "status": "SAVED",
  "createdAt": "2026-11-09T10:00:00Z"
}
```

**3. Mark it as sent:**

```bash
curl -X PATCH http://localhost:8080/api/applications/1/status \
  -H "Content-Type: application/json" \
  -d '{"status": "SENT"}'
```

The response is the same application with `"status": "SENT"`.

**4. A transition that is not allowed** returns `409`:

```bash
curl -X PATCH http://localhost:8080/api/applications/1/status \
  -H "Content-Type: application/json" \
  -d '{"status": "SAVED"}'
```

```json
{
  "title": "Conflict",
  "status": 409,
  "detail": "Cannot change status from SENT to SAVED",
  "instance": "/api/applications/1/status"
}
```

## Endpoints

| Method | Path | Description | Errors |
|---|---|---|---|
| GET | `/api/ping` | Health check, returns `pong` | — |
| GET | `/api/jobs?keyword=java&city=Göteborg` | Search job ads in JobTech, max 20. `city` is optional: Stockholm, Göteborg, Malmö or Uppsala (case-insensitive) | `400` invalid keyword or city · `502` JobTech error · `504` JobTech timeout |
| POST | `/api/applications` | Save a job application (`201`, status `SAVED`) | `400` invalid data · `409` duplicate JobTech ad |
| GET | `/api/applications` | List applications, newest first. Optional filter: `?status=SENT` | `400` invalid status |
| GET | `/api/applications/{id}` | Get one application | `404` not found |
| PATCH | `/api/applications/{id}/status` | Change the status of an application | `400` invalid status · `404` not found · `409` transition not allowed |
| DELETE | `/api/applications/{id}` | Delete an application (`204`) | `404` not found |

### Status transitions

| From | Allowed to |
|---|---|
| `SAVED` | `SENT` |
| `SENT` | `INTERVIEW`, `REJECTED` |
| `INTERVIEW` | `OFFER`, `REJECTED` |
| `REJECTED`, `OFFER` | — (final) |

An invalid transition returns `409 Conflict`. Setting the status an application already has also counts as an invalid transition.

### Errors

All errors are returned as [`ProblemDetail`](https://www.rfc-editor.org/rfc/rfc9457) JSON from a single `@RestControllerAdvice`. Validation errors include one message per invalid field or parameter:

```json
{
  "title": "Bad Request",
  "status": 400,
  "detail": "Invalid request data",
  "instance": "/api/applications",
  "errors": { "title": "must not be blank" }
}
```

A status that does not exist (in the `PATCH` body or in the `?status=` filter) returns `400` with the valid values:

```json
{
  "title": "Bad Request",
  "status": 400,
  "detail": "Invalid value 'FOO' for 'status'. Allowed values: SAVED, SENT, INTERVIEW, REJECTED, OFFER",
  "instance": "/api/applications"
}
```

## Project structure

```
io.github.anevadog.pendla
├── application/   job applications: controller, service, repository, entity, status enum, dto/
├── jobsearch/     JobTech search: controller, service, JobTech client and its config, supported cities, dto/
└── common/        global error handling (ProblemDetail) and Swagger config
```

The code is organized by feature: everything about one feature is in one package, and the layer is in each class name.

## Tests

```bash
./mvnw test
```

No database and no network needed:

- **Domain:** status transitions (all 25 combinations) and the entity's `changeStatus`, with plain JUnit.
- **Services:** business rules with Mockito mocks (duplicates, not found, transitions, city validation, mapping).
- **Controllers:** `@WebMvcTest` + MockMvc for status codes, validation and `ProblemDetail` responses.
- **JobTech client:** `MockRestServiceServer` simulates JobTech: success, server error, unreadable body, no connection and timeout.

## Known limitations

- Only four cities are supported (Stockholm, Göteborg, Malmö, Uppsala), mapped to their JobTech municipality ids.
- Some ads have several workplaces, so a search by city can include an ad whose main address is in another city.
- The database schema is managed by Hibernate (`ddl-auto=update`), which is fine for this MVP but not for production; versioned migrations (Flyway) would be the next step.
- Single user, local only: no authentication.
- Validation messages follow the language of the JVM.

## Data source

Job ads come from the [JobTech JobSearch API](https://jobsearch.api.jobtechdev.se/) by Arbetsförmedlingen. Pendla only stores the fields it needs (title, employer, city, link) and never stores contact persons' names, emails or phone numbers.
