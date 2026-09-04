package com.medisphere.consent.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Domain entity representing an explicit patient data access consent grant.
 * Persisted in the {@code consents} collection.
 */
@Document(collection = "consents")
@CompoundIndexes({
        @CompoundIndex(name = "patient_grantee_status_idx", def = "{'patientId': 1, 'grantedTo': 1, 'status': 1}"),
        @CompoundIndex(name = "patient_created_idx", def = "{'patientId': 1, 'createdAt': -1}")
})
public class Consent {

    @Id
    private String id;

    @Indexed
    private String patientId;

    @Indexed
    private String grantedTo;

    private String scope;

    private ConsentStatus status;

    private Instant grantedAt;
    private Instant revokedAt;
    private Instant expiresAt;
    private String reason;
    private Instant createdAt;
    private Instant updatedAt;

    public Consent() {
    }

    public Consent(String patientId, String grantedTo, String scope, ConsentStatus status,
                   Instant grantedAt, Instant expiresAt, String reason) {
        this.patientId = patientId;
        this.grantedTo = grantedTo;
        this.scope = scope;
        this.status = status;
        this.grantedAt = grantedAt != null ? grantedAt : Instant.now();
        this.expiresAt = expiresAt;
        this.reason = reason;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
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

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
