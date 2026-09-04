package com.medisphere.fhir.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Detailed ingestion result returned by {@code POST /api/fhir/ingest}.
 */
public class FhirIngestionResult {

    private int processedResources;
    private int validResources;
    private int invalidResources;
    private List<ResourceResult> results = new ArrayList<>();

    public FhirIngestionResult() {
    }

    public FhirIngestionResult(int processedResources, int validResources, int invalidResources, List<ResourceResult> results) {
        this.processedResources = processedResources;
        this.validResources = validResources;
        this.invalidResources = invalidResources;
        if (results != null) {
            this.results = results;
        }
    }

    public int getProcessedResources() {
        return processedResources;
    }

    public void setProcessedResources(int processedResources) {
        this.processedResources = processedResources;
    }

    public int getValidResources() {
        return validResources;
    }

    public void setValidResources(int validResources) {
        this.validResources = validResources;
    }

    public int getInvalidResources() {
        return invalidResources;
    }

    public void setInvalidResources(int invalidResources) {
        this.invalidResources = invalidResources;
    }

    public List<ResourceResult> getResults() {
        return results;
    }

    public void setResults(List<ResourceResult> results) {
        this.results = results;
    }

    public void addResult(ResourceResult result) {
        this.results.add(result);
        this.processedResources++;
        if ("VALID".equalsIgnoreCase(result.getStatus())) {
            this.validResources++;
        } else {
            this.invalidResources++;
        }
    }
}
