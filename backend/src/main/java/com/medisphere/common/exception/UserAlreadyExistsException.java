package com.medisphere.common.exception;

/**
 * Exception thrown when attempting to register a user with a username or email
 * that already exists in the system (HTTP 409 Conflict).
 */
public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
