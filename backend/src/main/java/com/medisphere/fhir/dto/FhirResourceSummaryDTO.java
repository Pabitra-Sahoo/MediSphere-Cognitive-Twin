package com.medisphere.fhir.dto;

import java.time.Instant;

/**
 * Summary DTO for FHIR resources returned in patient-scoped lists.
 */
public class FhirResourceSummaryDTO {

    private String id;
    private String resourceType;
    private String resourceId;
    private String validationStatus;
    private Instant processedAt;

    public FhirResourceSummaryDTO() {
    }

    public FhirResourceSummaryDTO(String id, String resourceType, String resourceId, String validationStatus, Instant processedAt) {
        this.id = id;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.validationStatus = validationStatus;
        this.processedAt = processedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(String validationStatus) {
        this.validationStatus = validationStatus;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }
}
