package com.proofchain.exception;

import java.time.LocalDateTime;
import java.util.Map;

public class ErrorResponse {

    private final boolean success = false;
    private final int status;
    private final String error;
    private final String message;
    private final LocalDateTime timestamp;
    private final Map<String, String> validationErrors;

    public ErrorResponse(int status, String error, String message) {
        this(status, error, message, null);
    }

    public ErrorResponse(int status, String error, String message, Map<String, String> validationErrors) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.validationErrors = validationErrors;
    }

    public boolean isSuccess() {
        return success;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Map<String, String> getValidationErrors() {
        return validationErrors;
    }
}
