package com.medisphere.twin.model;

import java.time.Instant;

/**
 * Nested sub-document representing latest lab values in the HealthTwin.
 */
public class TwinLabs {

    private Double glucose;
    private Double cholesterol;
    private Double hemoglobin;
    private Double creatinine;
    private Instant timestamp;

    public TwinLabs() {
    }

    public TwinLabs(Double glucose, Double cholesterol, Double hemoglobin, Double creatinine, Instant timestamp) {
        this.glucose = glucose;
        this.cholesterol = cholesterol;
        this.hemoglobin = hemoglobin;
        this.creatinine = creatinine;
        this.timestamp = timestamp;
    }

    public Double getGlucose() {
        return glucose;
    }

    public void setGlucose(Double glucose) {
        this.glucose = glucose;
    }

    public Double getCholesterol() {
        return cholesterol;
    }

    public void setCholesterol(Double cholesterol) {
        this.cholesterol = cholesterol;
    }

    public Double getHemoglobin() {
        return hemoglobin;
    }

    public void setHemoglobin(Double hemoglobin) {
        this.hemoglobin = hemoglobin;
    }

    public Double getCreatinine() {
        return creatinine;
    }

    public void setCreatinine(Double creatinine) {
        this.creatinine = creatinine;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
