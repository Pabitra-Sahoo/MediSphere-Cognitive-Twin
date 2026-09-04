package com.medisphere.vitals.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of data-quality boundary validation on a vital signs measurement packet.
 */
public class VitalsValidationResult {

    private final boolean valid;
    private final List<String> errors;
    private final List<String> warnings;

    public VitalsValidationResult(boolean valid, List<String> errors, List<String> warnings) {
        this.valid = valid;
        this.errors = errors != null ? Collections.unmodifiableList(new ArrayList<>(errors)) : Collections.emptyList();
        this.warnings = warnings != null ? Collections.unmodifiableList(new ArrayList<>(warnings)) : Collections.emptyList();
    }

    public static VitalsValidationResult success() {
        return new VitalsValidationResult(true, Collections.emptyList(), Collections.emptyList());
    }

    public static VitalsValidationResult failure(List<String> errors) {
        return new VitalsValidationResult(false, errors, Collections.emptyList());
    }

    public static VitalsValidationResult failure(List<String> errors, List<String> warnings) {
        return new VitalsValidationResult(false, errors, warnings);
    }

    public boolean isValid() {
        return valid;
    }

    public List<String> getErrors() {
        return errors;
    }

    public List<String> getWarnings() {
        return warnings;
    }
}
