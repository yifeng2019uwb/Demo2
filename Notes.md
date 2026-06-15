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
   - Host, Port, Database name, Username, Password

`ddl-auto=update` in the prod profile will auto-create tables on first startup — no manual SQL needed.

---

## Step 1 — Init project and add DTOs `[Engineering Quality · Operational Excellence]`

Go to https://start.spring.io/ and generate with:
- **Language**: Java | **Build**: Gradle
- **Dependencies**: Spring Web, Spring Data JPA, Validation, Actuator, Lombok, H2, PostgreSQL

In `build.gradle`, scope H2 to tests only:
```groovy
runtimeOnly 'org.postgresql:postgresql'
testRuntimeOnly 'com.h2database:h2'
```

Add a `Makefile`:
```makefile
build:
    ./gradlew build -x test
test:
    ./gradlew cleanTest test
run:
    ./gradlew bootRun
```

Define all request/response DTOs before writing any logic:
```java
public record CreateEventRequest(
    @NotBlank String customer_id,
    @NotBlank String event_type,
    @NotNull LocalDate timestamp,
    Map<String, String> metadata
) {}

public record CreateEventResponse(String event_id) {}
// GetEventResponse, GetSummaryResponse, ListTopEventsResponse ...
```

Configure profiles and observability in `src/main/resources/`:

**`application.properties`** (local / H2 default):
```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.hibernate.ddl-auto=create-drop
management.endpoints.web.exposure.include=health
management.endpoint.health.show-details=always
```
> Do NOT set `spring.datasource.driver-class-name` — let Spring auto-detect from the URL.

**`application-prod.properties`** (DO PostgreSQL):
```properties
spring.datasource.url=${SPRING_DATASOURCE_URL}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=update
management.endpoints.web.exposure.include=health
management.endpoint.health.show-details=always
```

---

## Step 2 — Controller (dummy responses) `[Engineering Quality]`

Create controller(s) with hardcoded dummy returns to verify routing compiles and works end-to-end before any real logic.

```java
@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    @PostMapping
    public ResponseEntity<CreateEventResponse> createEvent(@Valid @RequestBody CreateEventRequest request) {
        return ResponseEntity.status(201).body(new CreateEventResponse("dummy-id"));
    }

    @GetMapping("/{event_id}")
    public ResponseEntity<GetEventResponse> getEvent(@PathVariable String event_id) {
        return ResponseEntity.ok(new GetEventResponse(...));
    }

    @GetMapping("/summary")
    public ResponseEntity<GetSummaryResponse> getSummary(@ModelAttribute GetSummaryRequest request) {
        return ResponseEntity.ok(new GetSummaryResponse(...));
    }

    @GetMapping("/top-events")
    public ResponseEntity<ListTopEventsResponse> getTopEvents(@ModelAttribute ListTopEventsRequest request) {
        return ResponseEntity.ok(new ListTopEventsResponse(...));
    }
}
```

Run `make build` to confirm it compiles.

---

## Step 3 — Service interface and dummy implementation `[Engineering Quality]`

Define the service interface, then implement with dummy returns:

```java
public interface EventService {
    CreateEventResponse createEvent(CreateEventRequest request);
    GetEventResponse getEvent(String eventId);
    GetSummaryResponse getSummary(String customerId, LocalDate start, LocalDate end);
    ListTopEventsResponse getTopEvents(int limit);
}

@Service
public class EventServiceImpl implements EventService {
    @Override
    public CreateEventResponse createEvent(CreateEventRequest request) {
        return new CreateEventResponse("dummy-id");
    }
    // ...
}
```

Wire service into controller via constructor injection:
```java
private final EventService eventService;

public EventController(EventService eventService) {
    this.eventService = eventService;
}
```

Replace dummy controller returns with `eventService.xxx()` calls.

---

## Step 4 — Model and DAO `[Engineering Quality]`

**Entity:**
```java
@Entity
@Table(name = "events", indexes = {
    @Index(name = "idx_customer_id", columnList = "customer_id"),
    @Index(name = "idx_timestamp_desc", columnList = "timestamp DESC")
})
public class Event {
    @Id
    private String id;
    @Column(name = "customer_id") private String customerId;
    @Column(name = "type")        private String type;
    @Column(name = "timestamp")   private LocalDate timestamp;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")    private Map<String, String> metadata;

    protected Event() {}
    public Event(String customerId, String type, LocalDate timestamp, Map<String, String> metadata) {
        this.id = UUID.randomUUID().toString();
        // ...
    }
}
```

**DAO:**
```java
@Repository
public interface EventDao extends JpaRepository<Event, String> {
    Optional<Event> findEventById(String id);
    List<Event> findEventsByTimestampBetween(LocalDate start, LocalDate end);

    @Query("SELECT e FROM Event e ORDER BY e.timestamp DESC")
    List<Event> findTopEventsSortedByTimestampDesc(Pageable pageable);
}
```

---

## Step 5 — Implement service, add validation and error handling `[Engineering Quality]`

**Custom exceptions** in `exception/` package:
```java
public class ValidationException extends RuntimeException {
    public ValidationException(String message) { super(message); }
}
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) { super(message); }
}
```

**GlobalExceptionHandler** — maps exceptions to HTTP codes:
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, String>> handleValidation(ValidationException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NotFoundException e) {
        return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleMethodArgNotValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(Map.of("error", msg));
    }
}
```

**Service implementation** — inject DAO, validate inputs, throw exceptions, do NOT wrap in try/catch:
```java
@Service
public class EventServiceImpl implements EventService {
    private final EventDao eventDao;

    public EventServiceImpl(EventDao eventDao) { this.eventDao = eventDao; }

    @Override
    public CreateEventResponse createEvent(CreateEventRequest request) {
        validateUUID(request.customer_id());
        validateTimestamp(request.timestamp());
        Event saved = eventDao.save(new Event(request.customer_id(), request.event_type(), request.timestamp(), request.metadata()));
        return new CreateEventResponse(saved.getId());
    }

    @Override
    public GetEventResponse getEvent(String eventId) {
        validateUUID(eventId);
        Event e = eventDao.findEventById(eventId)
            .orElseThrow(() -> new NotFoundException("Event not found: " + eventId));
        return new GetEventResponse(e.getId(), e.getCustomerId(), e.getType(), e.getTimestamp(), e.getMetadata());
    }

    private void validateUUID(String uuid) {
        try { UUID.fromString(uuid); }
        catch (IllegalArgumentException e) { throw new ValidationException("Invalid UUID format"); }
    }

    private void validateTimestamp(LocalDate ts) {
        if (ts == null) throw new ValidationException("Timestamp cannot be null");
        if (ts.isAfter(LocalDate.now())) throw new ValidationException("Timestamp cannot be in the future");
    }
}
```

Run `make build && make test`.

---

## Step 6 — Unit tests `[Testing]`

Add service tests with Mockito covering happy path and all validation edge cases:

```java
@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {
    @Mock EventDao eventDao;
    @InjectMocks EventServiceImpl service;

    @Test
    void createEvent_savesAndReturnsId() {
        when(eventDao.save(any())).thenReturn(new Event(...));
        CreateEventResponse r = service.createEvent(new CreateEventRequest(VALID_UUID, "LOGIN", LocalDate.now(), null));
        assertThat(r.event_id()).isNotNull();
    }

    @Test
    void createEvent_throws_whenCustomerIdNotUuid() {
        assertThatThrownBy(() -> service.createEvent(new CreateEventRequest("bad-id", ...)))
            .isInstanceOf(ValidationException.class);
    }
    // ... cover all endpoints and edge cases
}
```

```bash
make test   # all tests must pass before deploying
```

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
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
```
> Bake `-Dspring.profiles.active=prod` into the ENTRYPOINT — more reliable than relying on env var injection.

### 7b — `.do/app.yml`
```yaml
name: my-service
region: sfo3          # must match the DB region
services:
  - name: api
    source_dir: /
    build_command: ./gradlew bootJar
    run_command: java -Dspring.profiles.active=prod -jar build/libs/*.jar
    http_port: 8080
    instance_count: 1
    instance_size_slug: basic-xxs
```

### 7c — CI `.github/workflows/ci.yml`
```yaml
name: CI
on:
  push:
    branches: [ main ]
env:
  FORCE_JAVASCRIPT_ACTIONS_TO_NODE24: true
jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: gradle
      - run: ./gradlew build -x test
      - run: ./gradlew test
```

### 7d — App environment variables (DO App Platform UI)
Go to App Platform → your app → service component → **Environment Variables**.
Set as **string values** (not toggles):

| Key | Value | Type |
|-----|-------|------|
| `SPRING_PROFILES_ACTIVE` | `prod` | Plain — **must be string, not boolean toggle** |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<host>:<port>/<db>?sslmode=require` | Plain |
| `SPRING_DATASOURCE_USERNAME` | your DB username | Plain |
| `SPRING_DATASOURCE_PASSWORD` | your DB password | **Encrypted** |

### 7e — Database environment (DO Database UI)
Go to **Databases → your cluster → Settings → Trusted Sources → Add** → select your App Platform app from the dropdown. DO handles the dynamic IP range automatically.

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

Verify health:
```
GET /actuator/health → { "status": "UP", "db": { "status": "UP" } }
```

---

## Step 8 — Integration tests `[Testing · Operational Excellence]`

Write an HTTP-level integration test while waiting for the deploy, then run it against the live URL:

```java
public class EventIT {
    static final String BASE_URL = "https://your-app.ondigitalocean.app";
    static final HttpClient client = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
        String eventId = createEvent();
        getEvent(eventId);
        getSummary();
        getTopEvents();
        System.out.println("All tests passed!");
    }
}
```

```bash
java -ea Integration_test/EventIT.java
```

After a successful run, verify data persisted in PostgreSQL:
```bash
psql "postgres://<user>:<password>@<host>:<port>/<db>?sslmode=require"
\dt          -- should show events table
SELECT * FROM events LIMIT 5;
```

---

## Common Pitfalls

| Problem | Cause | Fix |
|---------|-------|-----|
| App uses H2 on DO | `SPRING_PROFILES_ACTIVE` not set or set to boolean `true` | Set value to string `prod` in env vars UI |
| Connection timeout to DB | App Platform not in DB Trusted Sources | Add app from dropdown in DB → Settings → Trusted Sources |
| Tables not created | Wrong profile active — `create-drop` drops tables on shutdown | Ensure `prod` profile loads with `ddl-auto=update` |
| `driver-class-name` conflict | H2 driver set explicitly but PostgreSQL URL injected | Remove `spring.datasource.driver-class-name` from `application.properties` |
| Service throws `UnsupportedOperationException` | Try/catch swallowing `ValidationException` | Remove try/catch wrappers — let exceptions propagate naturally |




==================================================================================
POST /events
Body: {
  "customer_id": "cust-123",
  "event_type": "login",
  "timestamp": "2026-06-12T10:00:00Z",
  "metadata": { "ip": "10.0.0.1" }   ← optional
}
Response: 201 — { "event_id": "uuid" }


GET /events/{event_id}
Response: 200 — { event_id, customer_id, event_type, timestamp, metadata }
Response: 404 — if not found


GET /summary?customer_id=cust-123&start_time=...&end_time=...
Response: 200 — {
  "customer_id": "cust-123",
  "total_events": 150,
  "event_breakdown": { "login": 100, "purchase": 25, "logout": 25 }
}

GET /top-events?limit=10
Response: 200 — {
  "results": [
    { "event_type": "login", "count": 120 },
    { "event_type": "purchase", "count": 50 }
  ]
}

GET /health
Response: 200 — { "status": "healthy" }
