# Pendla

A small REST API to track my job search in Sweden.

Pendla does two things:

1. **Search real job ads** from the public JobTech API (Arbetsförmedlingen) by keyword and city, and return a simplified version of each ad (title, employer, city, link).
2. **Track my applications**: save the ads I am interested in and follow their status: `SAVED → SENT → INTERVIEW → REJECTED / OFFER`.

## Why I built it

I want to work in Sweden as a backend Java developer, and I use this tool in my own job search. It is also a portfolio project: small, finished, and every part of it explainable.

What it demonstrates:

- Layered architecture (controller, service, repository)
- DTOs: JPA entities are never exposed in HTTP responses
- Consuming an external API with timeouts and controlled errors
- Input validation and centralized error handling (`@ControllerAdvice` + `ProblemDetail`)
- Persistence with Spring Data JPA and PostgreSQL
- Unit and web-layer tests
- API documentation with Swagger (OpenAPI)

## Tech stack

Java 25 · Spring Boot · Maven · Spring Data JPA · PostgreSQL · Bean Validation · RestClient · springdoc-openapi · JUnit 5 · Mockito · MockMvc

## Status

In development. Current phase and next step: see [docs/plan.md](docs/plan.md).
Technical decisions and their alternatives: see [docs/decisiones.md](docs/decisiones.md) (Spanish).

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
5. Open Swagger UI at `http://localhost:8080/swagger-ui.html`.

Run the tests (no database needed):

```bash
./mvnw test
```

## Endpoints

| Method | Path | Description | Status |
|---|---|---|---|
| GET | `/api/ping` | Health check, returns `pong` | Done |
| GET | `/api/jobs?keyword=java&city=Göteborg` | Search job ads in JobTech | Planned |
| POST | `/api/applications` | Save a job application (`201`, `400` invalid data, `409` duplicate JobTech ad) | Done |
| GET | `/api/applications` | List applications (newest first) | Done |
| PATCH | `/api/applications/{id}/status` | Change the status of an application (`200`, `400` invalid status, `404` not found, `409` transition not allowed) | Done |
| DELETE | `/api/applications/{id}` | Delete an application (`204`, `404` not found) | Done |

Errors are returned as [`ProblemDetail`](https://www.rfc-editor.org/rfc/rfc9457) JSON. Validation errors include one message per invalid field:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Invalid request data",
  "instance": "/api/applications",
  "errors": { "title": "must not be blank" }
}
```

### Status transitions

| From | Allowed to |
|---|---|
| `SAVED` | `SENT` |
| `SENT` | `INTERVIEW`, `REJECTED` |
| `INTERVIEW` | `OFFER`, `REJECTED` |
| `REJECTED`, `OFFER` | — (final) |

An invalid transition returns `409 Conflict`. Setting the status an application already has also counts as an invalid transition.

## Data source

Job ads come from the [JobTech JobSearch API](https://jobsearch.api.jobtechdev.se/) by Arbetsförmedlingen. Pendla only stores the fields it needs (title, employer, city, link) and never stores contact persons' names, emails or phone numbers.
