package com.example.report.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/*
  "container_id": "web-1",
  "app_name": "billing-service",
  "cpu_usage_percent": 72.5,
  "memory_usage_mb": 512,
  "reported_at": "2026-06-15T10:00:00Z"
} */

public record CreateReportRequest(
    @NotBlank
    @JsonProperty("container_id")
    @Size(min = 12, max = 64, message = "Container ID must be between 12 and 64 characters")
    @Pattern(
        regexp = "^[a-zA-Z0-9_-]+$", 
        message = "Container ID can only contain alphanumeric characters, hyphens, and underscores"
    )
    String containerId,

    @NotBlank
    @JsonProperty("app_name")
    @Pattern(
        regexp = "^[a-zA-Z0-9._-]+$", 
        message = "App name can only contain alphanumeric characters, dots, hyphens, and underscores"
    )
    String appName,

    @JsonProperty("cpu_usage_percent")
    @DecimalMin(value = "0.0", message = "CPU usage must be at least 0.0")
    @DecimalMax(value = "1.0", message = "CPU usage cannot exceed 1.0")
    Double cpuUsagePercent,

    @JsonProperty("memory_usage_mb")
    @Positive(message = "Memory usage must be a positive number")
    Integer memoryUsageMb,

    @JsonProperty("reported_at")
    LocalDateTime reportedAt
) {
}