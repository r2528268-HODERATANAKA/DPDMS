package com.dpdms.zoonotic_disease_service.exception;

// Thrown when a record id does not exist.
// GlobalExceptionHandler turns this into HTTP 404 Not Found.
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
