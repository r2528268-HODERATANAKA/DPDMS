package com.dpdms.auth_service.exception;

// Requested record does not exist -> HTTP 404.
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
