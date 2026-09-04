package com.medisphere.common.exception;

/**
 * Thrown when an authenticated provider attempts to access protected patient data
 * without an active, non-expired consent grant.
 */
public class ConsentDeniedException extends RuntimeException {

    public ConsentDeniedException(String message) {
        super(message);
    }
}
