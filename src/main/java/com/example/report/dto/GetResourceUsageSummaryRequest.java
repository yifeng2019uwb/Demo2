package com.example.report.dto;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.format.annotation.DateTimeFormat;

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
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonProperty("start")
    OffsetDateTime start,

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonProperty("end")
    OffsetDateTime end
) {
    
}
