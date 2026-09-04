package com.medisphere.vitals.dto;

import com.medisphere.vitals.model.VitalsSource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * REST API response DTO representing a vital signs measurement record.
 */
public class VitalsDTO {

    private String id;
    private String patientId;
    private String eventId;
    private String deviceId;
    private Double heartRate;
    private Double systolicBP;
    private Double diastolicBP;
    private Double oxygenSaturation;
    private Double temperature;
    private Double respiratoryRate;
    private VitalsSource source;
    private boolean valid;
    private List<String> validationErrors = new ArrayList<>();
    private List<String> validationWarnings = new ArrayList<>();
    private Instant recordedAt;
    private Instant receivedAt;
    private Instant createdAt;

    public VitalsDTO() {
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

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Double getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(Double heartRate) {
        this.heartRate = heartRate;
    }

    public Double getSystolicBP() {
        return systolicBP;
    }

    public void setSystolicBP(Double systolicBP) {
        this.systolicBP = systolicBP;
    }

    public Double getDiastolicBP() {
        return diastolicBP;
    }

    public void setDiastolicBP(Double diastolicBP) {
        this.diastolicBP = diastolicBP;
    }

    public Double getOxygenSaturation() {
        return oxygenSaturation;
    }

    public void setOxygenSaturation(Double oxygenSaturation) {
        this.oxygenSaturation = oxygenSaturation;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getRespiratoryRate() {
        return respiratoryRate;
    }

    public void setRespiratoryRate(Double respiratoryRate) {
        this.respiratoryRate = respiratoryRate;
    }

    public VitalsSource getSource() {
        return source;
    }

    public void setSource(VitalsSource source) {
        this.source = source;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(List<String> validationErrors) {
        this.validationErrors = validationErrors != null ? validationErrors : new ArrayList<>();
    }

    public List<String> getValidationWarnings() {
        return validationWarnings;
    }

    public void setValidationWarnings(List<String> validationWarnings) {
        this.validationWarnings = validationWarnings != null ? validationWarnings : new ArrayList<>();
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
