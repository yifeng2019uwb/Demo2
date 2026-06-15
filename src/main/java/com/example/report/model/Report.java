package com.example.report.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/*
{
  "container_id": "web-1",
  "app_name": "billing-service",
  "cpu_usage_percent": 72.5,
  "memory_usage_mb": 512,
  "reported_at": "2026-06-15T10:00:00Z"
}

{ "report_id": "uuid" }
   */

@Entity
@Table(name = "reports_v2",
    indexes = {
        @Index(name = "idx_container_app", columnList = "containerId, appName"),
        @Index(name = "idx_reported_at", columnList = "reportedAt DESC")
    }
)
public class Report {
    
    @Id
    @Column(name = "report_id")
    private UUID reportId;

    @Column(name = "container_id")
    private String containerId;

    @Column(name = "app_name")
    private String appName;

    @Column(name = "cpu_usage_percent")
    private Double cpuUsagePercent;

    @Column(name = "memory_usage_mb")
    private Integer memoryUsageMb;

    @Column(name = "reported_at")
    private OffsetDateTime reportedAt;

    public Report() {
    }

    public Report(UUID reportId, String containerId, String appName, Double cpuUsagePercent, Integer memoryUsageMb, OffsetDateTime reportedAt) {
        this.reportId = reportId;
        this.containerId = containerId;
        this.appName = appName;
        this.cpuUsagePercent = cpuUsagePercent;
        this.memoryUsageMb = memoryUsageMb;
        this.reportedAt = reportedAt;
    }

    // Getter for report_id
    public UUID getReportId() {
        return this.reportId;
    }

    // Getter for containerId
    public String getContainerId() {
        return this.containerId;
    }

    // Getter for appName
    public String getAppName() {
        return this.appName;
    }

    // Getter for cpuUsagePercent
    public Double getCpuUsagePercent() {
        return this.cpuUsagePercent;
    }

    // Getter for memoryUsageMb()
    public Integer getMemoryUsageMb() {
        return this.memoryUsageMb;
    }

    // Getter for reportedAt
    public OffsetDateTime getReportedAt() {
        return this.reportedAt;
    }

    public UUID getId() {
        return this.reportId;
    }
    
}