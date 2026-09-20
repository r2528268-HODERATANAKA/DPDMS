package com.dpdms.auth_service.exception;

/** Bad username/password or disabled account -> HTTP 401. */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
