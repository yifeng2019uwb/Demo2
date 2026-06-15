package com.example.report.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class GetTopConsumersRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private Set<ConstraintViolation<GetTopConsumersRequest>> validate(GetTopConsumersRequest req) {
        return validator.validate(req);
    }

    private boolean hasViolationOn(Set<ConstraintViolation<GetTopConsumersRequest>> violations, String field) {
        return violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals(field));
    }

    @Test
    void validRequest_NoViolations() {
        assertTrue(validate(new GetTopConsumersRequest(10)).isEmpty());
    }

    @Test
    void limit_Zero_Violation() {
        assertTrue(hasViolationOn(validate(new GetTopConsumersRequest(0)), "limit"));
    }

    @Test
    void limit_Negative_Violation() {
        assertTrue(hasViolationOn(validate(new GetTopConsumersRequest(-1)), "limit"));
    }

    @Test
    void limit_ExactlyHundred_NoViolation() {
        assertFalse(hasViolationOn(validate(new GetTopConsumersRequest(100)), "limit"));
    }

    @Test
    void limit_Exceeds100_Violation() {
        assertTrue(hasViolationOn(validate(new GetTopConsumersRequest(101)), "limit"));
    }
}
