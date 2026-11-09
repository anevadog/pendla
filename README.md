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

Prerequisites: Java 25 and a local PostgreSQL database.

1. Copy `.env.example` to `.env` and fill in your local database values.
2. Start the application:

   ```bash
   ./mvnw spring-boot:run
   ```

3. Open Swagger UI at `http://localhost:8080/swagger-ui.html`.

Run the tests:

```bash
./mvnw test
```

## Endpoints

| Method | Path | Description | Status |
|---|---|---|---|
| GET | `/api/jobs?keyword=java&city=Göteborg` | Search job ads in JobTech | Planned |
| POST | `/api/applications` | Save a job application | Planned |
| GET | `/api/applications` | List applications (newest first) | Planned |
| PATCH | `/api/applications/{id}/status` | Change the status of an application | Planned |
| DELETE | `/api/applications/{id}` | Delete an application | Planned |

### Status transitions

| From | Allowed to |
|---|---|
| `SAVED` | `SENT` |
| `SENT` | `INTERVIEW`, `REJECTED` |
| `INTERVIEW` | `OFFER`, `REJECTED` |
| `REJECTED`, `OFFER` | — (final) |

An invalid transition returns `409 Conflict`.

## Data source

Job ads come from the [JobTech JobSearch API](https://jobsearch.api.jobtechdev.se/) by Arbetsförmedlingen. Pendla only stores the fields it needs (title, employer, city, link) and never stores contact persons' names, emails or phone numbers.
