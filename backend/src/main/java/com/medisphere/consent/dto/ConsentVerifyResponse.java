package com.medisphere.consent.dto;

import java.time.Instant;

/**
 * Response payload for provider consent verification checks.
 */
public class ConsentVerifyResponse {

    private boolean hasConsent;
    private String consentId;
    private String scope;
    private Instant expiresAt;

    public ConsentVerifyResponse() {
    }

    public ConsentVerifyResponse(boolean hasConsent, String consentId, String scope, Instant expiresAt) {
        this.hasConsent = hasConsent;
        this.consentId = consentId;
        this.scope = scope;
        this.expiresAt = expiresAt;
    }

    public static ConsentVerifyResponse noConsent() {
        return new ConsentVerifyResponse(false, null, null, null);
    }

    public boolean isHasConsent() {
        return hasConsent;
    }

    public void setHasConsent(boolean hasConsent) {
        this.hasConsent = hasConsent;
    }

    public String getConsentId() {
        return consentId;
    }

    public void setConsentId(String consentId) {
        this.consentId = consentId;
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
}
