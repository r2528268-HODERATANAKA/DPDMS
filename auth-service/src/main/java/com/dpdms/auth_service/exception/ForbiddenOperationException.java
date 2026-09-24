package com.dpdms.auth_service.exception;

// Caller is not allowed to do this (e.g. non-admin creating accounts) -> HTTP 403.
public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}
