package com.medisphere.audit.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Immutable audit log record for protected clinical, security, and consent operations.
 *
 * <p>Persisted in the {@code audit_logs} collection. Adheres to HIPAA-inspired
 * security logging practices by recording contextual metadata (actor, action, resource,
 * outcome, timestamp, IP) while strictly avoiding storage of passwords, JWT tokens,
 * or clinical observation telemetry payloads.</p>
 */
@Document(collection = "audit_logs")
@CompoundIndexes({
        @CompoundIndex(name = "timestamp_idx", def = "{'timestamp': -1}"),
        @CompoundIndex(name = "patient_timestamp_idx", def = "{'patientId': 1, 'timestamp': -1}"),
        @CompoundIndex(name = "user_timestamp_idx", def = "{'userId': 1, 'timestamp': -1}"),
        @CompoundIndex(name = "action_timestamp_idx", def = "{'action': 1, 'timestamp': -1}")
})
public class AuditLog {

    @Id
    private String id;

    private String userId;
    private String username;
    private String userRole;

    private AuditAction action;
    private String resourceType;
    private String resourceId;
    private String patientId;

    private String details;
    private AuditOutcome outcome;
    private String ipAddress;
    private Instant timestamp;

    public AuditLog() {
    }

    public AuditLog(String userId, String username, String userRole, AuditAction action,
                    String resourceType, String resourceId, String patientId,
                    String details, AuditOutcome outcome, String ipAddress, Instant timestamp) {
        this.userId = userId;
        this.username = username;
        this.userRole = userRole;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.patientId = patientId;
        this.details = details;
        this.outcome = outcome;
        this.ipAddress = ipAddress;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    public AuditAction getAction() {
        return action;
    }

    public void setAction(AuditAction action) {
        this.action = action;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public AuditOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(AuditOutcome outcome) {
        this.outcome = outcome;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
