package com.dpdms.fire_service.exception;

/** Requested resource does not exist -> HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
