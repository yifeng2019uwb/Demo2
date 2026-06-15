# Coding Interview Strategy
> A strategy for approaching a backend coding interview (3-hour live session).
> The goal is to always have something runnable and demonstrable at every stage, even if incomplete.

---

## What They're Evaluating

| Criterion | What to show |
|-----------|-------------|
| **Engineering Quality** | Clean code organization, sensible validation, meaningful error handling |
| **Testing** | Structured automated tests — unit tests with Mockito, integration tests against live service |
| **Automation & Workflow** | CI via GitHub Actions, Dockerfile, automated deploy on push to main |
| **Operational Excellence** | Observability (actuator health + DB status), configurable profiles (H2 local / PostgreSQL prod), scalable deployment config |

### Step → Criterion mapping

| Step | Criterion |
|------|-----------|
| Step 0 — Create DO database | Operational Excellence |
| Step 1 — Init, DTOs, profiles config | Engineering Quality · Operational Excellence |
| Step 2 — Controller (dummy) | Engineering Quality |
| Step 3 — Service interface + dummy impl | Engineering Quality |
| Step 4 — Model + DAO | Engineering Quality |
| Step 5 — Real impl + validation + error handling | Engineering Quality |
| Step 6 — Unit tests | Testing |
| Step 7 — Deploy (Dockerfile, app.yml, CI, env, DB trust) | Automation & Workflow · Operational Excellence |
| Step 8 — Integration tests | Testing · Operational Excellence |

---

## Step 0 — Check DO account and create the database `[Operational Excellence]`

Before writing any code, provision the PostgreSQL database so it's ready when you deploy.

1. Log in to **cloud.digitalocean.com**
2. **Databases → Create → PostgreSQL** — pick the same region you'll deploy the app to
3. Wait ~2 min for provisioning
4. Note the connection details (you'll need them in Step 7):
   - Host, Port, Database name (`defaultdb`), Username, Password

`ddl-auto=update` in the prod profile will auto-create tables on first startup — no manual SQL needed.

---

## Step 1 — Init project and add DTOs `[Engineering Quality · Operational Excellence]`

Go to https://start.spring.io/ and generate with:
- **Language**: Java | **Build**: Gradle
- **Dependencies**: Spring Web, Spring Data JPA, Validation, Actuator, Lombok, H2, PostgreSQL

In `build.gradle`, scope H2 to tests only so it is not included in the production runtime:
```groovy
runtimeOnly 'org.postgresql:postgresql'
testRuntimeOnly 'com.h2database:h2'
```

Define all request/response DTOs as Java records before writing any logic. This locks the API contract early and lets you compile-check the whole chain before any real implementation.

Configure two Spring profiles in `src/main/resources/`:

**`application.properties`** — default profile, used locally and in unit tests:
```properties
# In-memory H2 — no external dependency, resets on every restart
spring.datasource.url=jdbc:h2:mem:testdb

# create-drop: creates schema on startup, drops on shutdown — safe for local dev
spring.jpa.hibernate.ddl-auto=create-drop

# Expose only the health endpoint — never expose * in any environment
management.endpoints.web.exposure.include=health

# show-details=always makes /actuator/health include DB connectivity status
management.endpoint.health.show-details=always
```
> Do NOT set `spring.datasource.driver-class-name` here — Spring auto-detects from the H2 URL. Setting it explicitly conflicts when the prod profile injects a PostgreSQL URL.

**`application-prod.properties`** — prod profile, loaded on DO when `SPRING_PROFILES_ACTIVE=prod`:
```properties
spring.application.name=report

# Values injected at runtime from DO App Platform environment variables
spring.datasource.url=${SPRING_DATASOURCE_URL}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}

# Must be set explicitly for PostgreSQL — Spring cannot auto-detect without the URL at parse time
spring.datasource.driver-class-name=org.postgresql.Driver

# update: creates/alters tables without dropping data — NEVER use create-drop in prod
spring.jpa.hibernate.ddl-auto=update

management.endpoints.web.exposure.include=health
management.endpoint.health.show-details=always
```

---

## Step 2 — Controller (dummy responses) `[Engineering Quality]`

Create the controller with all routes returning hardcoded dummy values. This verifies routing, annotations, and DTO wiring compiles before writing any real logic.

- `@PostMapping` → return `ResponseEntity.status(201).body(...)`
- `@GetMapping("/{id}")` → `@PathVariable` for path params
- `@GetMapping("/summary")` → `@ModelAttribute` for query params (not `@RequestBody`)
- `@GetMapping("/top-consumers")` → `@ModelAttribute` with limit param

Run `make build` to confirm it compiles.

---

## Step 3 — Service interface and dummy implementation `[Engineering Quality]`

Define a service interface with one method per endpoint. Implement with dummy returns.

Wire the service into the controller via constructor injection (not field injection). Replace all dummy controller returns with `service.xxx()` calls. Run `make build` again.

---

## Step 4 — Model and DAO `[Engineering Quality]`

**Entity:** annotate with `@Entity`, `@Table`, `@Id`, `@Column`. Add `@Index` on columns used in query filters (e.g. `container_id`, `reported_at DESC`) — Hibernate generates these on `ddl-auto=update`.

**DAO:** extend `JpaRepository<Entity, IdType>`. Use Spring Data derived query method names for simple filters (`findByXAndYBetween`). Use `@Query` for anything that needs sorting or aggregation.

---

## Step 5 — Implement service, add validation and error handling `[Engineering Quality]`

Two custom exception classes in an `exception/` package: `ValidationException` (→ 400) and `NotFoundException` (→ 404).

`GlobalExceptionHandler` with `@RestControllerAdvice` maps exceptions to HTTP responses:
- `ValidationException` → `400 Bad Request`
- `NotFoundException` → `404 Not Found`
- `MethodArgumentNotValidException` → `400 Bad Request` with field-level error messages

Service implementation rules:
- Inject DAO via constructor, never field injection
- Validate inputs first, then call DAO
- Throw `NotFoundException` (not `ValidationException`) when a record is not found — "not found" is never a bad request
- Do NOT wrap logic in try/catch — let exceptions propagate to the handler naturally

Run `make build && make test`.

---

## Step 6 — Unit tests `[Testing]`

Use `@ExtendWith(MockitoExtension.class)` with `@Mock` on the DAO and `@InjectMocks` on the service.

Cover for each endpoint:
- Happy path: mock DAO to return a valid entity, assert response fields
- Not found: mock DAO to return empty Optional, assert `NotFoundException` is thrown
- Validation edge cases: null timestamp, future timestamp, invalid ranges

Run `make test` — all tests must pass before deploying.

---

## Step 7 — Deploy to DO App Platform `[Automation & Workflow · Operational Excellence]`

### 7a — Dockerfile

```dockerfile
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .
RUN ./gradlew bootJar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Key points:
- **Multi-stage build**: Stage 1 uses JDK (needs the compiler). Stage 2 uses JRE only — no compiler, ~200MB smaller image.
- **`*.jar` wildcard**: avoids hardcoding the jar filename, which is derived from `settings.gradle` project name + version. Hardcoding causes a build failure if the name ever changes.
- Profile activation is handled via the `SPRING_PROFILES_ACTIVE` env var in `app.yml`, not baked into the ENTRYPOINT.

### 7b — `.do/app.yml`

```yaml
name: my-service
region: sfo3          # must match the DB cluster region — cross-region = latency + egress cost
services:
  - name: api
    source_dir: /     # root of the repo, where build.gradle lives
    build_command: ./gradlew bootJar
    run_command: java -jar build/libs/*.jar
    http_port: 8080
    instance_count: 1             # increase for HA; 1 is fine for demo
    instance_size_slug: basic-xxs # smallest/cheapest (~$5/mo), sufficient for demo
    envs:
      - key: SPRING_PROFILES_ACTIVE
        scope: RUN_TIME           # injected at container startup, not during build
        value: prod
```

### 7c — CI `.github/workflows/ci.yml`

> **CRITICAL**: The directory must be `.github/workflows/` (plural). GitHub Actions silently ignores `.github/workflow/` (singular) — CI will never trigger.

```yaml
name: CI
on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]
env:
  FORCE_JAVASCRIPT_ACTIONS_TO_NODE24: true   # required for compatibility with newer actions
jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: gradle             # caches ~/.gradle between runs — speeds up subsequent CI significantly
      - run: ./gradlew build -x test
      - run: ./gradlew test
```

Build and test are separate steps so CI clearly shows which one failed.

### 7d — App environment variables (DO App Platform UI)

Go to **App Platform → your app → service component → Environment Variables**.

| Key | Value | Type |
|-----|-------|------|
| `SPRING_PROFILES_ACTIVE` | `prod` | Plain — **use the text field, not the toggle; the toggle sends `true` instead of `prod`** |
| `SPRING_DATASOURCE_URL` | see format below | Plain |
| `SPRING_DATASOURCE_USERNAME` | your DB username (from DO DB connection details) | Plain |
| `SPRING_DATASOURCE_PASSWORD` | your DB password | **Encrypted** — never store as plain text |

JDBC URL format:
```
jdbc:postgresql://<host>:<port>/<dbname>?sslmode=require
```
- `<dbname>` is `defaultdb` by default on DO managed PostgreSQL
- `sslmode=require` is mandatory — DO managed databases enforce SSL

### 7e — Database Trusted Sources (DO Database UI)

Go to **Databases → your cluster → Settings → Trusted Sources → Add** → select your App Platform app from the dropdown.

DO manages the dynamic IP range of App Platform automatically — you don't need to whitelist specific IPs. Without this step, all connections from your app will be refused.

### 7f — Push and verify

```bash
git add .
git commit -m "initial implementation"
git push origin main
```

DO deploys automatically on push (~2-3 min). Check startup logs for:
```
The following 1 profile is active: "prod"
HikariPool-1 - Added connection ... url=jdbc:postgresql://...
```

Verify health endpoint:
```
GET /actuator/health
→ { "status": "UP", "components": { "db": { "status": "UP" } } }
```

---

## Step 8 — Integration tests `[Testing · Operational Excellence]`

Write HTTP-level integration tests in `Integration-test/` as a standalone suite — external to the app, treating the deployed service as a black box.

### Java (plain `java` runner)

No build tool needed. Uses only the Java standard library (`java.net.http`). Run with:

```bash
java -ea Integration-test/EventIT.java

# Add to Makefile:
integ-test:
    java -ea Integration-test/EventIT.java
```

Use plain `assert` statements (enabled by `-ea`). No JUnit annotations — nothing calls `@Test` methods when run this way.

### Python (`pytest` + `requests`)

Place tests in `Integration-test/python/` as a standalone suite:

```
Integration-test/python/
  requirements.txt          # pytest, requests
  test_live_deployment.py
```

**`requirements.txt`:**
```
pytest
requests
```

**Makefile target** — creates a venv automatically (required on macOS/Homebrew Python due to PEP 668):
```makefile
integ-test-python:
    cd Integration-test/python && python3 -m venv .venv && .venv/bin/pip install -r requirements.txt -q && .venv/bin/pytest test_live_deployment.py -v
```

Run:
```bash
make integ-test-python

# Override base URL:
BASE_URL=https://your-app.ondigitalocean.app make integ-test-python
```

Add `.venv` to `.gitignore`.

> **Why venv?** macOS Homebrew Python enforces PEP 668 — `pip install` system-wide is blocked to protect the Homebrew Python installation. Always use a venv for project-level Python dependencies.

> **Timestamp pitfall**: Python `datetime.now()` uses local time; the server compares against its own `LocalDateTime.now()` (UTC on DO). A `timedelta(hours=2)` future timestamp may appear as past time to the server if your local timezone is behind UTC. Use `timedelta(days=1)` for future timestamp tests to safely clear any timezone offset.

### Verify DB persistence after tests

```bash
psql "postgres://<user>:<password>@<host>:<port>/<db>?sslmode=require"
\dt                        -- should show your tables
SELECT * FROM reports LIMIT 5;
```

---

## Common Pitfalls

| Problem | Cause | Fix |
|---------|-------|-----|
| App uses H2 on DO | `SPRING_PROFILES_ACTIVE` not set or set to boolean `true` | Set value to string `prod` using the text field, not the toggle |
| Connection refused to DB | App Platform not in DB Trusted Sources | Add app from dropdown in DB → Settings → Trusted Sources |
| Tables not created | Wrong profile active — `create-drop` drops tables on shutdown | Ensure `prod` profile is active so `ddl-auto=update` loads |
| `driver-class-name` conflict | H2 driver set explicitly in base properties but PostgreSQL URL injected | Remove `spring.datasource.driver-class-name` from `application.properties` |
| Service throws `UnsupportedOperationException` | Try/catch swallowing `ValidationException` | Remove try/catch wrappers — let exceptions propagate to `GlobalExceptionHandler` |
| CI never triggers | `.github/workflow/` directory (singular) is silently ignored | Rename to `.github/workflows/` (plural) |
| Build fails: jar not found | Hardcoded jar name in Dockerfile doesn't match `settings.gradle` project name | Use `*.jar` wildcard in `COPY --from=builder` |
| Timestamps stored/compared without timezone | `LocalDateTime` has no timezone — future-timestamp validation breaks when client and server are in different timezones | Use `OffsetDateTime` end-to-end (entity, all DTOs, DAO, service); maps to `timestamptz` in PostgreSQL |
| `spring.jackson.serialization.write-dates-as-timestamps=false` fails to bind | Spring Boot 4.x uses Jackson 3, where `WRITE_DATES_AS_TIMESTAMPS` moved from `SerializationFeature` to `DateTimeFeature` | Use `spring.jackson.json.datetime.write-dates-as-timestamps=false` instead |
