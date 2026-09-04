package com.medisphere.twin.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Nested sub-document representing the calculated completeness of a HealthTwin.
 * Evaluates the 20 logical fields required for M1.
 */
public class TwinCompleteness {

    private double percentage;
    private List<String> missingFields = new ArrayList<>();
    private int totalFields = 20;
    private int populatedFields;
    private Instant calculatedAt;

    public TwinCompleteness() {
        this.calculatedAt = Instant.now();
    }

    public TwinCompleteness(double percentage, List<String> missingFields, int totalFields, int populatedFields) {
        this.percentage = percentage;
        this.missingFields = missingFields != null ? missingFields : new ArrayList<>();
        this.totalFields = totalFields;
        this.populatedFields = populatedFields;
        this.calculatedAt = Instant.now();
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public List<String> getMissingFields() {
        return missingFields;
    }

    public void setMissingFields(List<String> missingFields) {
        this.missingFields = missingFields != null ? missingFields : new ArrayList<>();
    }

    public int getTotalFields() {
        return totalFields;
    }

    public void setTotalFields(int totalFields) {
        this.totalFields = totalFields;
    }

    public int getPopulatedFields() {
        return populatedFields;
    }

    public void setPopulatedFields(int populatedFields) {
        this.populatedFields = populatedFields;
    }

    public Instant getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(Instant calculatedAt) {
        this.calculatedAt = calculatedAt;
    }
}
