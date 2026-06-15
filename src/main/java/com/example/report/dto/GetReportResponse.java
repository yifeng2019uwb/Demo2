package com.example.report.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/*
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
 */
public record GetReportResponse (
    UUID report_id,
    String container_id,
    String app_name,
    Double cpu_usage_percent,
    Integer memory_usage_mb,
    OffsetDateTime reported_at
) {
}
