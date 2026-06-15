package com.example.report.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;

/*
top-consumers
 */
public record GetTopConsumersRequest (
    @JsonProperty("limit")
    @Positive(message = "Limit must be a positive number")
    @Max(value = 100, message = "Limit must be less than or equal to 100")
    Integer limit
) {
}
