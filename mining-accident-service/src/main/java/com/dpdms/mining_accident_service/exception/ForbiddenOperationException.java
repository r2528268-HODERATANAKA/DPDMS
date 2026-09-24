package com.dpdms.mining_accident_service.exception;

// Thrown when the caller is not allowed to do the action
// (for example a drought recorder trying to save a fire incident).
// GlobalExceptionHandler turns this into HTTP 403 Forbidden.
public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}
