package com.example.report.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;

import org.springframework.web.bind.annotation.BindParam;

/*
top-consumers
 */
public record GetTopConsumersRequest (
    @Positive(message = "Limit must be a positive number")
    @Max(value = 100, message = "Limit must be less than or equal to 100")
    @BindParam("limit")
    Integer limit
) {
}
