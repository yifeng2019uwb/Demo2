package com.example.report.exception;

public class ValidationException extends RuntimeException {

    private static final String ERROR_CODE = "VALIDATION_ERROR";

    public ValidationException(String message) {
        super(message);
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }

}
