# Practice Interview Challenges
> Four realistic "data ingestion and processing REST API" challenges in the style of a DigitalOcean interview.
> Each is scoped for a 3-hour build + deploy session.

---

## Challenge 1 — URL Health Monitor

### Background
You are building an internal reliability service. Agents deployed across infrastructure periodically run health checks against URLs and report their results to your API. The service must ingest those results and provide summary statistics for operational dashboards.

### Data Model
Each health check report contains:
- `url` — the URL that was checked
- `status_code` — HTTP status code returned (e.g. 200, 404, 500)
- `response_time_ms` — how long the request took in milliseconds
- `checked_at` — ISO-8601 timestamp of when the check was run

### API Requirements

**POST** `/api/v1/checks`
Ingest a single health check result.
```json
{
  "url": "https://example.com/api",
  "status_code": 200,
  "response_time_ms": 143,
  "checked_at": "2026-06-15T10:00:00Z"
}
```
Response `201`:
```json
{ "check_id": "uuid" }
```

**GET** `/api/v1/checks/{check_id}`
Retrieve a single check result by ID. Returns `404` if not found.

**GET** `/api/v1/checks/stats?url=<url>&start=<date>&end=<date>`
Return aggregated statistics for a given URL over a time range.
```json
{
  "url": "https://example.com/api",
  "total_checks": 240,
  "uptime_percentage": 98.5,
  "avg_response_time_ms": 152,
  "min_response_time_ms": 80,
  "max_response_time_ms": 3200
}
```

**GET** `/api/v1/checks/slowest?limit=10`
Return the top N slowest URLs by average response time.
```json
{
  "results": [
    { "url": "https://slow.example.com", "avg_response_time_ms": 1850 },
    { "url": "https://example.com/heavy", "avg_response_time_ms": 920 }
  ]
}
```

### Validation
- `url` must be non-blank
- `status_code` must be between 100 and 599
- `response_time_ms` must be a positive integer
- `checked_at` must not be in the future

### Notes
- Uptime is defined as the percentage of checks where `status_code` is in the 2xx range
- `limit` for the slowest endpoint must be between 1 and 100

---

## Challenge 2 — Container Resource Reports

### Background
You are building a resource monitoring service for a container platform. Container agents emit periodic resource snapshots to your API. The service must store those snapshots and expose aggregated usage metrics for capacity planning and alerting.

### Data Model
Each resource report contains:
- `container_id` — unique identifier of the container (string)
- `app_name` — name of the application running in the container
- `cpu_usage_percent` — CPU usage as a percentage (0.0–100.0)
- `memory_usage_mb` — memory consumed in megabytes
- `reported_at` — ISO-8601 timestamp of the snapshot

### API Requirements

**POST** `/api/v1/reports`
Ingest a resource snapshot from a container agent.
```json
{
  "container_id": "web-1",
  "app_name": "billing-service",
  "cpu_usage_percent": 72.5,
  "memory_usage_mb": 512,
  "reported_at": "2026-06-15T10:00:00Z"
}
```
Response `201`:
```json
{ "report_id": "uuid" }
```

**GET** `/api/v1/reports/{report_id}`
Retrieve a single report by ID. Returns `404` if not found.

**GET** `/api/v1/reports/summary?app_name=<name>&start=<date>&end=<date>`
Return resource usage summary for a given app over a time range.
```json
{
  "app_name": "billing-service",
  "total_reports": 1440,
  "avg_cpu_percent": 65.2,
  "peak_cpu_percent": 98.1,
  "avg_memory_mb": 480,
  "peak_memory_mb": 1024
}
```

**GET** `/api/v1/reports/top-consumers?limit=10`
Return the top N containers by average CPU usage.
```json
{
  "results": [
    { "container_id": "worker-3", "avg_cpu_percent": 91.2 },
    { "container_id": "web-1",    "avg_cpu_percent": 72.5 }
  ]
}
```

### Validation
- `container_id` and `app_name` must be non-blank
- `cpu_usage_percent` must be between 0.0 and 100.0
- `memory_usage_mb` must be a positive integer
- `reported_at` must not be in the future

### Notes
- A container is considered "high CPU" if its average exceeds 80%
- `limit` for top-consumers must be between 1 and 100

---

## Challenge 3 — API Rate Tracker

### Background
You are building an API usage tracking service. Each time a customer makes a request to any platform API, a usage event is recorded. The service must ingest those events and expose usage analytics used for rate limiting dashboards, billing, and abuse detection.

### Data Model
Each usage event contains:
- `api_key` — the API key used for the request (string)
- `endpoint` — the API path called (e.g. `/v2/droplets`)
- `method` — HTTP method (`GET`, `POST`, `DELETE`, etc.)
- `status_code` — response status code
- `occurred_at` — ISO-8601 timestamp of the request

### API Requirements

**POST** `/api/v1/usage`
Ingest a single API usage event.
```json
{
  "api_key": "do_abc123",
  "endpoint": "/v2/droplets",
  "method": "POST",
  "status_code": 201,
  "occurred_at": "2026-06-15T10:00:00Z"
}
```
Response `201`:
```json
{ "event_id": "uuid" }
```

**GET** `/api/v1/usage/{event_id}`
Retrieve a single usage event by ID. Returns `404` if not found.

**GET** `/api/v1/usage/summary?api_key=<key>&start=<date>&end=<date>`
Return usage summary for a given API key over a time range.
```json
{
  "api_key": "do_abc123",
  "total_requests": 4500,
  "error_rate_percent": 2.1,
  "breakdown": {
    "/v2/droplets": 3000,
    "/v2/domains":  1500
  }
}
```

**GET** `/api/v1/usage/top-consumers?limit=10`
Return the top N API keys by total request count.
```json
{
  "results": [
    { "api_key": "do_abc123", "total_requests": 4500 },
    { "api_key": "do_xyz789", "total_requests": 3100 }
  ]
}
```

### Validation
- `api_key` and `endpoint` must be non-blank
- `method` must be one of: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`
- `status_code` must be between 100 and 599
- `occurred_at` must not be in the future

### Notes
- Error rate is the percentage of requests where `status_code` is 4xx or 5xx
- `limit` for top-consumers must be between 1 and 100

---

## Challenge 4 — Deployment History Service

### Background
You are building a deployment tracking service for a CI/CD platform. Every time an application is deployed, the deployment pipeline reports the outcome to your API. The service must store deployment records and provide analytics for reliability dashboards and post-mortem reviews.

### Data Model
Each deployment record contains:
- `application` — name of the application deployed (string)
- `version` — version string being deployed (e.g. `1.4.2`)
- `status` — outcome of the deployment: `success`, `failed`, or `rolled_back`
- `duration_seconds` — how long the deployment took
- `deployed_at` — ISO-8601 timestamp of the deployment

### API Requirements

**POST** `/api/v1/deployments`
Ingest a deployment record.
```json
{
  "application": "billing-service",
  "version": "1.4.2",
  "status": "success",
  "duration_seconds": 142,
  "deployed_at": "2026-06-15T10:00:00Z"
}
```
Response `201`:
```json
{ "deployment_id": "uuid" }
```

**GET** `/api/v1/deployments/{deployment_id}`
Retrieve a single deployment by ID. Returns `404` if not found.

**GET** `/api/v1/deployments/summary?application=<name>&start=<date>&end=<date>`
Return deployment analytics for a given application over a time range.
```json
{
  "application": "billing-service",
  "total_deployments": 42,
  "success_rate_percent": 92.8,
  "avg_duration_seconds": 138,
  "breakdown": {
    "success":     39,
    "failed":       2,
    "rolled_back":  1
  }
}
```

**GET** `/api/v1/deployments/top-failing?limit=10`
Return the top N applications with the highest failure rate.
```json
{
  "results": [
    { "application": "legacy-api", "failure_rate_percent": 35.0 },
    { "application": "data-worker", "failure_rate_percent": 12.5 }
  ]
}
```

### Validation
- `application` and `version` must be non-blank
- `status` must be one of: `success`, `failed`, `rolled_back`
- `duration_seconds` must be a positive integer
- `deployed_at` must not be in the future

### Notes
- Failure rate includes both `failed` and `rolled_back` statuses
- `limit` for top-failing must be between 1 and 100
