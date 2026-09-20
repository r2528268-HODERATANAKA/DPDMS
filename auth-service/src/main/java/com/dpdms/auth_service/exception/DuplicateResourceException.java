package com.dpdms.auth_service.exception;

/** Resource already exists -> HTTP 409. */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
