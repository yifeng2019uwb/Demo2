package com.example.report.dto;

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
public record GetResourceUsageSummaryResponse (
    String container_id,
    String app_name,
    Integer total_reports,
    Double avg_cpu_percent,
    Double peak_cpu_percent,
    Integer avg_memory_mb,
    Integer peak_memory_mb
) {
}
