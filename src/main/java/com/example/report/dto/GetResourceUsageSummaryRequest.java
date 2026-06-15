package com.example.report.dto;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.BindParam;

/*
**GET** `/api/v1/reports/summary?app_name=<name>&start=<date>&end=<date>`
 */

public record GetResourceUsageSummaryRequest (

    @NotBlank
    @BindParam("container_id")
    String containerId,

    @NotBlank
    @BindParam("app_name")
    String appName,

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @BindParam("start")
    OffsetDateTime start,

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @BindParam("end")
    OffsetDateTime end
) {
    
}
