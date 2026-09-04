package com.medisphere.twin.dto;

import com.medisphere.twin.model.TwinDemographics;
import com.medisphere.twin.model.TwinFhirSyncStatus;
import com.medisphere.twin.model.TwinLabs;
import com.medisphere.twin.model.TwinVitals;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class HealthTwinDTO {

    private String id;
    private String patientId;
    private TwinDemographics demographics;
    private TwinVitals latestVitals;
    private TwinLabs latestLabs;
    private TwinFhirSyncStatus fhirSyncStatus;
    private TwinCompletenessDTO completeness;
    private Map<String, Object> riskScores = new HashMap<>();
    private Instant createdAt;
    private Instant updatedAt;

    public HealthTwinDTO() {
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

    public TwinCompletenessDTO getCompleteness() {
        return completeness;
    }

    public void setCompleteness(TwinCompletenessDTO completeness) {
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
