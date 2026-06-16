# Container Resource Monitor

REST API for ingesting and querying container resource usage snapshots. Built with Spring Boot 4.1.0, deployed on DigitalOcean App Platform with managed PostgreSQL.

## Stack

- Java 21 · Spring Boot 4.1.0 · Spring Data JPA · Gradle
- PostgreSQL (prod) · H2 (local/test)
- Docker multi-stage build · GitHub Actions CI · DigitalOcean App Platform

## Project Structure

```
src/main/java/com/example/report/
  controller/   REST layer — routing, request binding, response codes
  service/      Business logic — validation, aggregation, top-consumer ranking
  dao/          Spring Data JPA repositories
  dto/          Request/response records (Java records)
  model/        JPA entity (reports_v2 table, UUID PK, timestamptz columns)
  exception/    ValidationException, NotFoundException, GlobalExceptionHandler

src/main/resources/
  application.properties        Local profile — H2 in-memory, create-drop
  application-prod.properties   Prod profile — PostgreSQL via env vars, ddl-auto=update

src/test/java/com/example/report/
  service/      Unit tests — Mockito, all methods + attribute assertions
  controller/   MockMvc standaloneSetup — routing, validation, HTTP status codes
  dto/          Jakarta Validator — constraint tests per field
  exception/    Direct handler unit tests — BindException, ValidationException, NotFoundException

Integration-test/python/
  test_live_deployment.py   External pytest suite against live deployed service
```

## Commands

```bash
make run          # Run locally (H2, no DB setup needed)
make build        # Build without tests
make test         # Unit tests + JaCoCo coverage report
make integ-test-python  # Integration tests against deployed service
```

Coverage report: `build/reports/jacoco/test/html/index.html`

## Deployment

Push to `main` → GitHub Actions builds and tests → DigitalOcean deploys automatically.

See [Notes.md](Notes.md) for full step-by-step deploy instructions, DB schema decisions to lock down before first deploy, environment variable setup, and common pitfalls.

## Future Improvements

- **`getTopConsumers` full implementation** — currently returns sorted avg CPU by container; could add time-range filter and app_name grouping
- **Pagination** — `GET /api/v1/reports` list endpoint with cursor-based pagination for large datasets
- **Custom Micrometer metrics** — counter for reports ingested, histogram for CPU distribution per container
- **Structured JSON logging** — switch to Logback JSON encoder in prod for log aggregation (Datadog / Papertrail)
- **Repository layer tests** — `@DataJpaTest` with H2 to verify derived query methods and date range filtering
- **Rate limiting** — protect the ingest endpoint from agent storms flooding the DB
