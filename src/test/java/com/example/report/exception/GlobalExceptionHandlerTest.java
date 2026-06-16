package com.example.report.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleValidation_Returns400WithMessage() {
        var response = handler.handleValidation(new ValidationException("Timestamp cannot be in the future"));

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Timestamp cannot be in the future", response.getBody().get("error"));
    }

    @Test
    void handleNotFound_Returns404WithMessage() {
        var response = handler.handleNotFound(new NotFoundException("Report not found with ID: abc"));

        assertEquals(404, response.getStatusCode().value());
        assertEquals("Report not found with ID: abc", response.getBody().get("error"));
    }

    @Test
    void handleBind_SingleFieldError_Returns400WithFieldAndMessage() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "containerId", "must not be blank"));
        BindException ex = new BindException(bindingResult);

        var response = handler.handleBind(ex);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("containerId: must not be blank", response.getBody().get("error"));
    }

    @Test
    void handleBind_MultipleFieldErrors_Returns400WithAllFieldsJoined() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "containerId", "must not be blank"));
        bindingResult.addError(new FieldError("request", "appName", "must not be blank"));
        BindException ex = new BindException(bindingResult);

        var response = handler.handleBind(ex);

        assertEquals(400, response.getStatusCode().value());
        String error = response.getBody().get("error");
        assertTrue(error.contains("containerId: must not be blank"));
        assertTrue(error.contains("appName: must not be blank"));
    }
}
