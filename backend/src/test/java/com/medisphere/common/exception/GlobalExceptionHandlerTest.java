package com.medisphere.common.exception;

import com.medisphere.common.dto.ApiError;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ApiError and GlobalExceptionHandler foundations.
 */
class GlobalExceptionHandlerTest {

    @Test
    void apiError_shouldPopulateTimestamp() {
        ApiError error = new ApiError(404, "Not Found", "Patient not found", "/api/patients/123");

        assertNotNull(error.timestamp());
        assertEquals(404, error.status());
        assertEquals("Not Found", error.error());
        assertEquals("Patient not found", error.message());
        assertEquals("/api/patients/123", error.path());
        assertNull(error.details());
    }

    @Test
    void apiError_withDetails_shouldIncludeFieldErrors() {
        var details = java.util.List.of("name: must not be blank", "email: invalid format");
        ApiError error = new ApiError(400, "Bad Request", "Validation failed", "/api/patients", details);

        assertEquals(2, error.details().size());
        assertTrue(error.details().contains("name: must not be blank"));
    }

    @Test
    void resourceNotFoundException_shouldFormatMessage() {
        var ex = new ResourceNotFoundException("Patient", "abc-123");
        assertEquals("Patient not found: abc-123", ex.getMessage());
    }
}
