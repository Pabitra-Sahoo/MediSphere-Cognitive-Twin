package com.medisphere.audit.dto;

import com.medisphere.audit.model.AuditAction;
import com.medisphere.audit.model.AuditLog;
import com.medisphere.audit.model.AuditOutcome;

import java.time.Instant;

/**
 * Data Transfer Object for {@link AuditLog} representations in REST responses.
 */
public class AuditLogDTO {

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

    public AuditLogDTO() {
    }

    public AuditLogDTO(AuditLog log) {
        if (log != null) {
            this.id = log.getId();
            this.userId = log.getUserId();
            this.username = log.getUsername();
            this.userRole = log.getUserRole();
            this.action = log.getAction();
            this.resourceType = log.getResourceType();
            this.resourceId = log.getResourceId();
            this.patientId = log.getPatientId();
            this.details = log.getDetails();
            this.outcome = log.getOutcome();
            this.ipAddress = log.getIpAddress();
            this.timestamp = log.getTimestamp();
        }
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
