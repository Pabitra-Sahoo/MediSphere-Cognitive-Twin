package com.medisphere.fhir.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB document representing a persisted FHIR resource in the {@code fhir_resources} collection.
 */
@Document(collection = "fhir_resources")
public class FhirResource {

    @Id
    private String id;

    @Indexed
    private String patientId;

    @Indexed
    private String resourceType;

    @Indexed
    private String resourceId;

    private String rawJson;
    private String version = "R4";
    private String validationStatus; // VALID, INVALID
    private List<String> validationErrors = new ArrayList<>();
    private Instant processedAt;
    private Instant receivedAt;

    public FhirResource() {
        this.receivedAt = Instant.now();
        this.processedAt = Instant.now();
    }

    public FhirResource(String patientId, String resourceType, String resourceId,
                        String rawJson, String validationStatus, List<String> validationErrors) {
        this();
        this.patientId = patientId;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.rawJson = rawJson;
        this.validationStatus = validationStatus;
        if (validationErrors != null) {
            this.validationErrors = validationErrors;
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

    public String getRawJson() {
        return rawJson;
    }

    public void setRawJson(String rawJson) {
        this.rawJson = rawJson;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(String validationStatus) {
        this.validationStatus = validationStatus;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(List<String> validationErrors) {
        this.validationErrors = validationErrors;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }
}
