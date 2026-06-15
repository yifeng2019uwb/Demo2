package com.example.report.dto;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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

    @NotNull
    @JsonProperty("start")
    OffsetDateTime start,
    
    @NotNull
    @JsonProperty("end")
    OffsetDateTime end
) {
    
}
