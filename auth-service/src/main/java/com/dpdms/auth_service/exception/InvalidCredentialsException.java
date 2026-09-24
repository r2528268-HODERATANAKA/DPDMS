package com.dpdms.auth_service.exception;

// Bad username/password or a deactivated account -> HTTP 401.
// Same message for wrong password and unknown user so attackers cannot
// find out which usernames exist.
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
