package com.medisphere.common.exception;

/**
 * Thrown when a requested resource is not found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceType, String identifier) {
        super(resourceType + " not found: " + identifier);
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
