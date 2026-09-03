package com.medisphere.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Standard API error response.
 *
 * <p>All error responses from the API follow this consistent structure.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path,
    List<String> details
) {
    public ApiError(int status, String error, String message, String path) {
        this(Instant.now(), status, error, message, path, null);
    }

    public ApiError(int status, String error, String message, String path, List<String> details) {
        this(Instant.now(), status, error, message, path, details);
    }
}
