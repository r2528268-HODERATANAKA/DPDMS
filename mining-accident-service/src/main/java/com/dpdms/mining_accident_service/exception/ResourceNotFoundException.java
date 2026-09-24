package com.dpdms.mining_accident_service.exception;

// Thrown when a record id does not exist.
// GlobalExceptionHandler turns this into HTTP 404 Not Found.
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
