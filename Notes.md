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
| **Operational Excellence** | Observability (actuator health + DB status), configurable profiles, scalable deployment config |

### Step plan and timing

| Time | Step | Criterion | Note |
|------|------|-----------|------|
| 0:00 | Step 0 — Provision DO DB | Operational Excellence | Starts in background while you code |
| 0:20 | Step 1 — DTOs + profiles | Engineering Quality · Operational Excellence | |
| 0:30 | Step 2 — Controller (dummy returns) | Engineering Quality | |
| 0:40 | Step 3 — Service (dummy returns) | Engineering Quality | |
| 1:00 | Step 4 — Model + DAO | Engineering Quality | **Deploy here** — DO build takes ~3 min |
| 1:30 | Step 5 — Real impl + validation + error handling | Engineering Quality | |
| 2:00 | Step 6 — Unit tests | Testing | |
| 2:20 | Step 8 — Integration tests | Testing · Operational Excellence | Requires live service |
| 2:20+ | Polish — README, alerts, health checks, demo | Automation & Workflow · Operational Excellence | |

**Checkpoints — glance at these only:**

| Clock | Must be done |
|-------|-------------|
| 1:00 | Model + DAO done, **deploy triggered** |
| 1:30 | Service impl done, app live on DO |
| 2:00 | Unit tests done |
| 2:30 | Integration tests done, demo ready |

**Why deploy at Step 4:** `ddl-auto=update` creates the DB table on first startup — lock down the schema before then. Wrong column type (`LocalDateTime` → `timestamp` instead of `timestamptz`) or PK type requires a table rename or migration to fix. Don't leave deploy to the last 20 minutes — infrastructure issues are unpredictable.

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

Scope H2 to tests only in `build.gradle` so it is not included in the production runtime.

Define all request/response DTOs as Java records before writing any logic. This locks the API contract early and lets you compile-check the whole chain before any real implementation.

Configure two Spring profiles — see `src/main/resources/application.properties` (H2, local) and `application-prod.properties` (PostgreSQL, DO).

> **Pitfall**: Do NOT set `spring.datasource.driver-class-name` in the base properties — Spring auto-detects from the H2 URL. Setting it explicitly conflicts when the prod profile injects a PostgreSQL URL.

---

## Step 2 — Controller (dummy responses) `[Engineering Quality]`

Create the controller with all routes returning hardcoded dummy values. Verifies routing and DTO wiring compiles before writing any real logic.

- `@PostMapping` → `ResponseEntity.status(201).body(...)`
- `@GetMapping("/{id}")` → `@PathVariable`
- `@GetMapping("/summary")` → `@ModelAttribute` for query params (not `@RequestBody`)
- `@GetMapping("/top-consumers")` → `@ModelAttribute` with limit param

Run `make build` to confirm it compiles.

---

## Step 3 — Service interface and dummy implementation `[Engineering Quality]`

Define a service interface with one method per endpoint. Implement with dummy returns.

Wire the service into the controller via constructor injection (not field injection). Replace all dummy controller returns with `service.xxx()` calls. Run `make build` again.

---

## Step 4 — Model and DAO `[Engineering Quality]`

See `src/main/java/com/example/report/model/` and `dao/`.

- **Entity**: UUID PK, `OffsetDateTime` for timestamps (maps to `timestamptz`), `@Index` on query filter columns
- **DAO**: extend `JpaRepository`. Use Spring Data derived query names for simple filters; `@Query` for aggregation.

> **Lock down before first deploy**: PK type and timestamp type are hard to change after the table is created. `OffsetDateTime` → `timestamptz`. `LocalDateTime` → `timestamp` (no timezone) — breaks future-timestamp validation across timezones.

---

## Step 5 — Implement service, add validation and error handling `[Engineering Quality]`

See `src/main/java/com/example/report/service/` and `exception/`.

`GlobalExceptionHandler` (`@RestControllerAdvice`) maps exceptions to HTTP responses:
- `ValidationException` → `400`
- `NotFoundException` → `404`
- `MethodArgumentNotValidException` → `400` (field errors from `@RequestBody @Valid`)
- `BindException` → `400` (field errors from `@ModelAttribute @Valid`)

Service rules:
- Constructor injection, never field injection
- Validate inputs first, then call DAO
- `NotFoundException` (not `ValidationException`) when a record is not found
- No try/catch — let exceptions propagate to the handler naturally

Run `make build && make test`.

---

## Step 6 — Unit tests `[Testing]`

See `src/test/java/com/example/report/`.

- `service/` — Mockito, all methods, assert every response field
- `controller/` — MockMvc `standaloneSetup` with `GlobalExceptionHandler` wired in
- `dto/` — Jakarta `Validator` directly, one test per constraint
- `exception/` — call handler methods directly with constructed exceptions

Run `make test`.

---

## Step 7 — Deploy to DO App Platform `[Automation & Workflow · Operational Excellence]`

### 7a — Dockerfile

See `Dockerfile` — multi-stage build (JDK builder → JRE runtime, ~200MB smaller). Uses `*.jar` wildcard to avoid hardcoding the jar name from `settings.gradle`.

### 7b — `.do/app.yml`

See `.do/app.yml` — region must match DB cluster region. `SPRING_PROFILES_ACTIVE=prod` injected at `RUN_TIME` (not build time).

### 7c — CI

See `.github/workflows/ci.yml` — build and test as separate steps so CI shows which one failed.

> **CRITICAL**: Directory must be `.github/workflows/` (plural). GitHub Actions silently ignores `.github/workflow/` (singular) — CI will never trigger.

### 7d — App environment variables (DO App Platform UI)

Go to **App Platform → your app → Settings → Environment Variables**.

| Key | Type | Note |
|-----|------|------|
| `SPRING_PROFILES_ACTIVE` | Plain | Use the text field, not the toggle — toggle sends `true` instead of `prod` |
| `SPRING_DATASOURCE_URL` | Plain | `jdbc:postgresql://<host>:<port>/defaultdb?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | Plain | From DO DB connection details |
| `SPRING_DATASOURCE_PASSWORD` | **Encrypted** | Never plain text |

### 7e — After app is created, before first working deploy

The first deploy will fail with a DB connection error until you complete these two steps:

1. **DB Trusted Sources** — Databases → your cluster → Settings → Trusted Sources → Add → select your app
2. **Env vars** — add all 4 vars above (Step 7d)

Then: **App Platform → your app → Actions → Force Rebuild and Deploy**

### 7f — Verify

Check startup logs for `prod` profile active and HikariPool connected.

```
GET /actuator/health → { "status": "UP", "components": { "db": { "status": "UP" } } }
```

---

## Step 8 — Integration tests `[Testing · Operational Excellence]`

See `Integration-test/python/test_live_deployment.py`.

```bash
make integ-test-python

# Override base URL:
BASE_URL=https://your-app.ondigitalocean.app make integ-test-python
```

> **venv required** — macOS Homebrew Python blocks system-wide `pip install` (PEP 668). The Makefile creates a venv automatically.

> **Timestamp pitfall** — use `datetime.now(timezone.utc)` with `strftime('%Y-%m-%dT%H:%M:%SZ')`. Plain `datetime.now()` is local time and may be rejected by the server's UTC comparison.

---

## Step 9 — Monitoring and Alerts (DO App Platform) `[Operational Excellence]`

App Platform → your app → **Insights** (metrics) and **Alerts**.

### Alert policies

| Metric | Condition | Duration | Why |
|--------|-----------|----------|-----|
| **Restart Count** | above 1 | 1 min | Any restart = crash, OOM, or health check failure |
| **Response Time (P95)** | above 1000ms | 5 mins | Tail latency degrading — DB slowness or GC pause. Healthy baseline is 50–200ms |
| **CPU** | above 85% | 5 mins | Sustained high CPU means overloaded. Short spikes are normal for JVM |
| **RAM** | above 85% | 10 mins | Growing without dropping = memory leak |

Duration = how long condition must be true before alert fires. Too short → noisy. Too long → misses real incidents.

### Health Checks

Go to **App Platform → your app → component → Health Checks**. Add both Readiness and Liveness pointing at `/actuator/health`.

- **TCP** — only checks if port is open. App could be returning 500s and TCP still passes.
- **HTTP** — makes a real GET request. `/actuator/health` checks DB connectivity.

| Field | Readiness | Liveness |
|-------|-----------|----------|
| Type | HTTP | HTTP |
| Path | `/actuator/health` | `/actuator/health` |
| Initial Delay | 30s | 60s |
| Period | 10s | 10s |
| Timeout | 5s | 5s |
| Failure Threshold | 3 | 3 |

> Initial Delay 0 triggers false failures during Spring Boot startup (~15–20s to initialize).

---

## Architecture & Decision Review (30 min)

Don't follow the Notes step by step. Let the interviewer steer; go deep when they ask follow-up questions.

### 1. Demo first (5 min)

Start with the running system — not code.

```
GET  /actuator/health              → DB status UP
POST /api/v1/reports               → 201 + UUID
GET  /api/v1/reports/{id}          → all fields
GET  /api/v1/reports/summary       → avg/peak aggregation
GET  /api/v1/reports/top-consumers → ranked results
```

### 2. Structure tour (3 min)

> "Controller handles routing only. Service has all business logic. DAO is pure Spring Data. DTOs are records — immutable, no boilerplate. No logic leaks between layers."

### 3. Key decisions (17 min)

Name the decision and the tradeoff — let them ask for depth:

- `OffsetDateTime` vs `LocalDateTime`
- UUID PK vs auto-increment
- Centralized `GlobalExceptionHandler`
- Profile-based config (H2 local, PostgreSQL prod)
- `@ModelAttribute` + `@BindParam` for query params

### 4. Gaps and tradeoffs (5 min)

- **`getTopConsumers` full table scan** — add time-range filter or pre-aggregated table at scale
- **No authentication** — add API key or JWT at gateway level
- **No pagination** — add cursor-based pagination on list endpoints
- **`ddl-auto=update` in prod** — use Flyway for explicit versioned migrations in production
- **Tests written after deploy** — deploy early in a time-boxed interview; infrastructure issues are unpredictable

---

## Common Pitfalls

| Problem | Cause | Fix |
|---------|-------|-----|
| App uses H2 on DO | `SPRING_PROFILES_ACTIVE` not set or set to boolean `true` | Use the text field, not the toggle — sends `prod` not `true` |
| Connection refused to DB | App not in DB Trusted Sources | Add app from dropdown in DB → Settings → Trusted Sources |
| Tables not created | Wrong profile — `create-drop` drops tables on shutdown | Ensure `prod` profile is active so `ddl-auto=update` loads |
| `driver-class-name` conflict | H2 driver set explicitly in base properties | Remove from `application.properties` — only set in prod profile |
| CI never triggers | `.github/workflow/` (singular) silently ignored | Rename to `.github/workflows/` (plural) |
| Build fails: jar not found | Hardcoded jar name in Dockerfile | Use `*.jar` wildcard in `COPY --from=builder` |
| Timestamps break across timezones | `LocalDateTime` has no timezone | Use `OffsetDateTime` end-to-end → `timestamptz` in PostgreSQL |
| Jackson datetime serialization fails | Spring Boot 4.x uses Jackson 3 — property path changed | Use `spring.jackson.json.datetime.write-dates-as-timestamps=false` |

Future Improvements (for discussion)
Authentication
Pagination
Caching
Monitoring/alerts
CI/CD
Horizontal scaling