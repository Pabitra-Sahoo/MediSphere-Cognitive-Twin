package com.medisphere.twin.model;

import java.time.Instant;

/**
 * Nested sub-document representing latest vital signs in the HealthTwin.
 */
public class TwinVitals {

    private Double heartRate;
    private Double systolicBP;
    private Double diastolicBP;
    private Double oxygenSaturation;
    private Double temperature;
    private Double respiratoryRate;
    private Instant timestamp;

    public TwinVitals() {
    }

    public TwinVitals(Double heartRate, Double systolicBP, Double diastolicBP,
                      Double oxygenSaturation, Double temperature, Double respiratoryRate, Instant timestamp) {
        this.heartRate = heartRate;
        this.systolicBP = systolicBP;
        this.diastolicBP = diastolicBP;
        this.oxygenSaturation = oxygenSaturation;
        this.temperature = temperature;
        this.respiratoryRate = respiratoryRate;
        this.timestamp = timestamp;
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

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
