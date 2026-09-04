package com.medisphere.twin.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * HealthTwin document stored in the {@code health_twins} collection.
 * Maintains a 1-to-1 relationship with a Patient.
 */
@Document(collection = "health_twins")
public class HealthTwin {

    @Id
    private String id;

    @Indexed(unique = true)
    private String patientId;

    private TwinDemographics demographics;
    private TwinVitals latestVitals;
    private TwinLabs latestLabs;
    private TwinFhirSyncStatus fhirSyncStatus;
    private TwinCompleteness completeness;

    /**
     * Placeholder map for future M2 federated learning risk scores.
     * Initialized as empty map for M1.
     */
    private Map<String, Object> riskScores = new HashMap<>();

    private Instant createdAt;
    private Instant updatedAt;

    public HealthTwin() {
        this.demographics = new TwinDemographics();
        this.latestVitals = new TwinVitals();
        this.latestLabs = new TwinLabs();
        this.fhirSyncStatus = new TwinFhirSyncStatus();
        this.completeness = new TwinCompleteness();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public HealthTwin(String patientId) {
        this();
        this.patientId = patientId;
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

    public TwinDemographics getDemographics() {
        return demographics;
    }

    public void setDemographics(TwinDemographics demographics) {
        this.demographics = demographics;
    }

    public TwinVitals getLatestVitals() {
        return latestVitals;
    }

    public void setLatestVitals(TwinVitals latestVitals) {
        this.latestVitals = latestVitals;
    }

    public TwinLabs getLatestLabs() {
        return latestLabs;
    }

    public void setLatestLabs(TwinLabs latestLabs) {
        this.latestLabs = latestLabs;
    }

    public TwinFhirSyncStatus getFhirSyncStatus() {
        return fhirSyncStatus;
    }

    public void setFhirSyncStatus(TwinFhirSyncStatus fhirSyncStatus) {
        this.fhirSyncStatus = fhirSyncStatus;
    }

    public TwinCompleteness getCompleteness() {
        return completeness;
    }

    public void setCompleteness(TwinCompleteness completeness) {
        this.completeness = completeness;
    }

    public Map<String, Object> getRiskScores() {
        return riskScores;
    }

    public void setRiskScores(Map<String, Object> riskScores) {
        this.riskScores = riskScores != null ? riskScores : new HashMap<>();
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
