package com.dpdms.auth_service.exception;

// Duplicate username on account creation -> HTTP 409 Conflict.
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
