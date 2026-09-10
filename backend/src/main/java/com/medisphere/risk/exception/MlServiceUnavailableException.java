package com.medisphere.risk.exception;

/**
 * Thrown when the internal Python ML service is unreachable, timed out, or returns a 5xx error.
 * Results in HTTP 503 Service Unavailable.
 */
public class MlServiceUnavailableException extends RuntimeException {

    public MlServiceUnavailableException(String message) {
        super(message);
    }

    public MlServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
