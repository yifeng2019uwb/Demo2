package com.example.report.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.time.OffsetDateTime;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class CreateReportRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private Set<ConstraintViolation<CreateReportRequest>> validate(CreateReportRequest req) {
        return validator.validate(req);
    }

    private boolean hasViolationOn(Set<ConstraintViolation<CreateReportRequest>> violations, String field) {
        return violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals(field));
    }

    // ==========================================
    // Valid request
    // ==========================================

    @Test
    void validRequest_NoViolations() {
        var req = new CreateReportRequest("web-1", "billing-service", 72.5, 512, OffsetDateTime.now());
        assertTrue(validate(req).isEmpty());
    }

    // ==========================================
    // container_id
    // ==========================================

    @Test
    void containerId_Blank_Violation() {
        var req = new CreateReportRequest("", "billing-service", 72.5, 512, OffsetDateTime.now());
        assertTrue(hasViolationOn(validate(req), "containerId"));
    }

    @Test
    void containerId_TooShort_Violation() {
        var req = new CreateReportRequest("ab", "billing-service", 72.5, 512, OffsetDateTime.now());
        assertTrue(hasViolationOn(validate(req), "containerId"));
    }

    @Test
    void containerId_TooLong_Violation() {
        String longId = "a".repeat(65);
        var req = new CreateReportRequest(longId, "billing-service", 72.5, 512, OffsetDateTime.now());
        assertTrue(hasViolationOn(validate(req), "containerId"));
    }

    @Test
    void containerId_InvalidCharsAtSign_Violation() {
        var req = new CreateReportRequest("web@1", "billing-service", 72.5, 512, OffsetDateTime.now());
        assertTrue(hasViolationOn(validate(req), "containerId"));
    }

    @Test
    void containerId_InvalidCharsSpace_Violation() {
        var req = new CreateReportRequest("web 1", "billing-service", 72.5, 512, OffsetDateTime.now());
        assertTrue(hasViolationOn(validate(req), "containerId"));
    }

    @Test
    void containerId_HyphenUnderscore_NoViolation() {
        var req = new CreateReportRequest("web_1-abc", "billing-service", 72.5, 512, OffsetDateTime.now());
        assertFalse(hasViolationOn(validate(req), "containerId"));
    }

    // ==========================================
    // app_name
    // ==========================================

    @Test
    void appName_Blank_Violation() {
        var req = new CreateReportRequest("web-1", "", 72.5, 512, OffsetDateTime.now());
        assertTrue(hasViolationOn(validate(req), "appName"));
    }

    @Test
    void appName_TooShort_Violation() {
        var req = new CreateReportRequest("web-1", "ab", 72.5, 512, OffsetDateTime.now());
        assertTrue(hasViolationOn(validate(req), "appName"));
    }

    @Test
    void appName_TooLong_Violation() {
        String longName = "a".repeat(65);
        var req = new CreateReportRequest("web-1", longName, 72.5, 512, OffsetDateTime.now());
        assertTrue(hasViolationOn(validate(req), "appName"));
    }

    @Test
    void appName_InvalidCharsAtSign_Violation() {
        var req = new CreateReportRequest("web-1", "billing@service", 72.5, 512, OffsetDateTime.now());
        assertTrue(hasViolationOn(validate(req), "appName"));
    }

    @Test
    void appName_DotsHyphensUnderscores_NoViolation() {
        var req = new CreateReportRequest("web-1", "billing.service_v2-prod", 72.5, 512, OffsetDateTime.now());
        assertFalse(hasViolationOn(validate(req), "appName"));
    }

    // ==========================================
    // cpu_usage_percent
    // ==========================================

    @Test
    void cpu_NegativeValue_Violation() {
        var req = new CreateReportRequest("web-1", "billing-service", -1.0, 512, OffsetDateTime.now());
        assertTrue(hasViolationOn(validate(req), "cpuUsagePercent"));
    }

    @Test
    void cpu_Exceeds100_Violation() {
        var req = new CreateReportRequest("web-1", "billing-service", 100.1, 512, OffsetDateTime.now());
        assertTrue(hasViolationOn(validate(req), "cpuUsagePercent"));
    }

    @Test
    void cpu_ZeroValue_NoViolation() {
        var req = new CreateReportRequest("web-1", "billing-service", 0.0, 512, OffsetDateTime.now());
        assertFalse(hasViolationOn(validate(req), "cpuUsagePercent"));
    }

    @Test
    void cpu_ExactlyHundred_NoViolation() {
        var req = new CreateReportRequest("web-1", "billing-service", 100.0, 512, OffsetDateTime.now());
        assertFalse(hasViolationOn(validate(req), "cpuUsagePercent"));
    }

    // ==========================================
    // memory_usage_mb
    // ==========================================

    @Test
    void memory_Zero_Violation() {
        var req = new CreateReportRequest("web-1", "billing-service", 72.5, 0, OffsetDateTime.now());
        assertTrue(hasViolationOn(validate(req), "memoryUsageMb"));
    }

    @Test
    void memory_Negative_Violation() {
        var req = new CreateReportRequest("web-1", "billing-service", 72.5, -1, OffsetDateTime.now());
        assertTrue(hasViolationOn(validate(req), "memoryUsageMb"));
    }

    @Test
    void memory_PositiveValue_NoViolation() {
        var req = new CreateReportRequest("web-1", "billing-service", 72.5, 1, OffsetDateTime.now());
        assertFalse(hasViolationOn(validate(req), "memoryUsageMb"));
    }

    // ==========================================
    // reported_at
    // ==========================================

    @Test
    void reportedAt_Null_Violation() {
        var req = new CreateReportRequest("web-1", "billing-service", 72.5, 512, null);
        assertTrue(hasViolationOn(validate(req), "reportedAt"));
    }
}
