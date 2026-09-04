package com.medisphere.fhir.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Result representation of an individual ingested FHIR resource.
 */
public class ResourceResult {

    private String resourceType;
    private String resourceId;
    private String status; // VALID, INVALID
    private String action; // MAPPED, REJECTED, PERSISTED
    private String patientId;
    private List<String> errors = new ArrayList<>();

    public ResourceResult() {
    }

    public ResourceResult(String resourceType, String resourceId, String status, String action, List<String> errors) {
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.status = status;
        this.action = action;
        if (errors != null) {
            this.errors = errors;
        }
    }

    public ResourceResult(String resourceType, String resourceId, String status, String action, String patientId, List<String> errors) {
        this(resourceType, resourceId, status, action, errors);
        this.patientId = patientId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
