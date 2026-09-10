package com.medisphere.risk.exception;

import java.util.Collections;
import java.util.List;

/**
 * Thrown when required clinical parameters (from HealthTwin or EncounterContext) are missing.
 * Results in HTTP 422 Unprocessable Entity.
 */
public class InsufficientClinicalDataException extends RuntimeException {

    private final List<String> missingFeatures;

    public InsufficientClinicalDataException(String message, List<String> missingFeatures) {
        super(message);
        this.missingFeatures = missingFeatures != null ? missingFeatures : Collections.emptyList();
    }

    public List<String> getMissingFeatures() {
        return missingFeatures;
    }

    public List<String> getMissingFields() {
        return missingFeatures;
    }
}
