package com.medisphere.lab.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Entity representing laboratory test results stored in {@code lab_results} collection.
 */
@Document(collection = "lab_results")
public class LabResult {

    @Id
    private String id;

    @Indexed
    private String patientId;

    private String testName;
    private String testCode;
    private Double value;
    private String unit;
    private ReferenceRange referenceRange;
    private String status; // FINAL, PRELIMINARY, CANCELLED
    private Instant performedAt;
    private Instant reportedAt;
    private String source; // FHIR, MANUAL

    public LabResult() {
        this.reportedAt = Instant.now();
    }

    public LabResult(String patientId, String testName, String testCode, Double value,
                     String unit, ReferenceRange referenceRange, String status,
                     Instant performedAt, String source) {
        this.patientId = patientId;
        this.testName = testName;
        this.testCode = testCode;
        this.value = value;
        this.unit = unit;
        this.referenceRange = referenceRange;
        this.status = status;
        this.performedAt = performedAt;
        this.reportedAt = Instant.now();
        this.source = source;
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

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getTestCode() {
        return testCode;
    }

    public void setTestCode(String testCode) {
        this.testCode = testCode;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public ReferenceRange getReferenceRange() {
        return referenceRange;
    }

    public void setReferenceRange(ReferenceRange referenceRange) {
        this.referenceRange = referenceRange;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getPerformedAt() {
        return performedAt;
    }

    public void setPerformedAt(Instant performedAt) {
        this.performedAt = performedAt;
    }

    public Instant getReportedAt() {
        return reportedAt;
    }

    public void setReportedAt(Instant reportedAt) {
        this.reportedAt = reportedAt;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    /**
     * Value object representing normal low/high bounds for a lab test.
     */
    public static class ReferenceRange {
        private Double low;
        private Double high;

        public ReferenceRange() {
        }

        public ReferenceRange(Double low, Double high) {
            this.low = low;
            this.high = high;
        }

        public Double getLow() {
            return low;
        }

        public void setLow(Double low) {
            this.low = low;
        }

        public Double getHigh() {
            return high;
        }

        public void setHigh(Double high) {
            this.high = high;
        }
    }
}
