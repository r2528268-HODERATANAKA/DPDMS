package com.dpdms.auth_service.exception;

/** Caller is authenticated but out of scope -> HTTP 403. */
public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}
