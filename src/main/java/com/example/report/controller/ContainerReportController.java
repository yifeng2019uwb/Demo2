package com.example.report.controller;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.report.dto.GetReportResponse;
import com.example.report.dto.GetResourceUsageSummaryRequest;
import com.example.report.dto.GetResourceUsageSummaryResponse;
import com.example.report.dto.GetTopConsumersRequest;
import com.example.report.dto.GetTopConsumersResponse;
import com.example.report.service.ReportService;

import jakarta.validation.Valid;

import com.example.report.dto.CreateReportResponse;
import com.example.report.dto.CreateReportRequest;

/*
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
 */


@RestController
@RequestMapping("/api/v1/reports")
public class ContainerReportController {

    private static final Logger log = LoggerFactory.getLogger(ContainerReportController.class);
    private final ReportService reportService;

    public ContainerReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    public ResponseEntity<CreateReportResponse> postReport(@Valid @RequestBody CreateReportRequest request) {
        // Implementation for posting a new report
        return ResponseEntity.status(201).body(reportService.createReport(request));
    }

    @GetMapping("/{report_id}")
    public ResponseEntity<GetReportResponse> getReport(@Valid@PathVariable UUID report_id) {
        // Implementation for retrieving a single report
        return ResponseEntity.ok(reportService.getReport(report_id));
    }

    @GetMapping("/summary")
    public ResponseEntity<GetResourceUsageSummaryResponse> getSummary(
            @Valid @ModelAttribute GetResourceUsageSummaryRequest request) {
        // Implementation for retrieving a summary of reports
        return ResponseEntity.ok(reportService.getResourceUsageSummary(request));
    }

    @GetMapping("/top-consumers")
    public ResponseEntity<GetTopConsumersResponse> getTopConsumers(@Valid @ModelAttribute GetTopConsumersRequest request) {
        // Implementation for retrieving top-consuming containers
        return ResponseEntity.ok(reportService.getTopConsumers(request));
    }

}
