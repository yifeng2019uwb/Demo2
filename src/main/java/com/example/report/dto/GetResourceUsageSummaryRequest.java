package com.example.report.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;

/*
**GET** `/api/v1/reports/summary?app_name=<name>&start=<date>&end=<date>`
 */

public record GetResourceUsageSummaryRequest (

    @NotBlank
    @JsonProperty("container_id")
    String containerId,

    @NotBlank
    @JsonProperty("app_name")
    String appName,

    @NotBlank
    @JsonProperty("start")
    LocalDateTime start,
    
    @NotBlank
    @JsonProperty("end")
    LocalDateTime end
) {
    
}
