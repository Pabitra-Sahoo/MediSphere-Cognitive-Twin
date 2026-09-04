package com.medisphere.consent.dto;

import com.medisphere.consent.model.Consent;
import com.medisphere.consent.model.ConsentStatus;

import java.time.Instant;

/**
 * DTO representing patient consent data in API responses.
 */
public class ConsentDTO {

    private String id;
    private String patientId;
    private String grantedTo;
    private String grantedToName;
    private String scope;
    private ConsentStatus status;
    private Instant grantedAt;
    private Instant expiresAt;
    private Instant revokedAt;
    private String reason;

    public ConsentDTO() {
    }

    public ConsentDTO(Consent consent, String grantedToName) {
        if (consent != null) {
            this.id = consent.getId();
            this.patientId = consent.getPatientId();
            this.grantedTo = consent.getGrantedTo();
            this.grantedToName = grantedToName != null ? grantedToName : consent.getGrantedTo();
            this.scope = consent.getScope();
            this.status = consent.getStatus();
            this.grantedAt = consent.getGrantedAt();
            this.expiresAt = consent.getExpiresAt();
            this.revokedAt = consent.getRevokedAt();
            this.reason = consent.getReason();
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getGrantedTo() {
        return grantedTo;
    }

    public void setGrantedTo(String grantedTo) {
        this.grantedTo = grantedTo;
    }

    public String getGrantedToName() {
        return grantedToName;
    }

    public void setGrantedToName(String grantedToName) {
        this.grantedToName = grantedToName;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public ConsentStatus getStatus() {
        return status;
    }

    public void setStatus(ConsentStatus status) {
        this.status = status;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(Instant grantedAt) {
        this.grantedAt = grantedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
