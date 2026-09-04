package com.medisphere.consent.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

/**
 * Request payload for creating/granting a new patient consent.
 */
public class ConsentCreateRequest {

    @NotBlank(message = "grantedTo provider identifier is required")
    private String grantedTo;

    @NotBlank(message = "Consent scope is required (e.g. treatment, patient-privacy)")
    private String scope;

    private Instant expiresAt;

    private String reason;

    public ConsentCreateRequest() {
    }

    public ConsentCreateRequest(String grantedTo, String scope, Instant expiresAt) {
        this.grantedTo = grantedTo;
        this.scope = scope;
        this.expiresAt = expiresAt;
    }

    public ConsentCreateRequest(String grantedTo, String scope, Instant expiresAt, String reason) {
        this.grantedTo = grantedTo;
        this.scope = scope;
        this.expiresAt = expiresAt;
        this.reason = reason;
    }

    public String getGrantedTo() {
        return grantedTo;
    }

    public void setGrantedTo(String grantedTo) {
        this.grantedTo = grantedTo;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
