package com.medisphere.fhir.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import ca.uhn.fhir.validation.ValidationResult;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Consent;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for validating FHIR R4 resources using HAPI FHIR validation
 * and domain integrity constraints.
 */
@Service
public class FhirValidationService {

    private static final Logger log = LoggerFactory.getLogger(FhirValidationService.class);

    private final FhirContext fhirContext;
    private final FhirValidator fhirValidator;

    public FhirValidationService(FhirContext fhirContext, FhirValidator fhirValidator) {
        this.fhirContext = fhirContext;
        this.fhirValidator = fhirValidator;
    }

    /**
     * Validates a parsed FHIR R4 resource against structural schema and domain rules.
     *
     * @param resource the FHIR resource to validate
     * @return ValidationOutcome containing validity flag and detailed error messages
     */
    public ValidationOutcome validate(IBaseResource resource) {
        if (resource == null) {
            return new ValidationOutcome(false, List.of("Resource is null"));
        }

        List<String> errors = new ArrayList<>();

        // 1. HAPI FHIR Schema / Structural validation
        try {
            ValidationResult valResult = fhirValidator.validateWithResult(resource);
            for (SingleValidationMessage msg : valResult.getMessages()) {
                if (msg.getSeverity() == ResultSeverityEnum.ERROR || msg.getSeverity() == ResultSeverityEnum.FATAL) {
                    errors.add((msg.getLocationString() != null ? msg.getLocationString() + ": " : "") + msg.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("HAPI FHIR validator threw exception: {}", e.getMessage());
            errors.add("Structural validation exception: " + e.getMessage());
        }

        // 2. Domain-specific mandatory field validation
        validateDomainIntegrity(resource, errors);

        boolean isValid = errors.isEmpty();
        return new ValidationOutcome(isValid, errors);
    }

    private void validateDomainIntegrity(IBaseResource resource, List<String> errors) {
        if (resource instanceof Patient patient) {
            if (!patient.hasName() && !patient.hasIdentifier()) {
                errors.add("Missing required field: Patient must have at least one name or identifier");
            }
        } else if (resource instanceof Observation obs) {
            if (!obs.hasStatus()) {
                errors.add("Missing required field: Observation status is mandatory");
            }
            if (!obs.hasCode() || obs.getCode().getCoding().isEmpty()) {
                errors.add("Missing required field: Observation code with at least one coding is mandatory");
            }
        } else if (resource instanceof DiagnosticReport report) {
            if (!report.hasStatus()) {
                errors.add("Missing required field: DiagnosticReport status is mandatory");
            }
            if (!report.hasCode()) {
                errors.add("Missing required field: DiagnosticReport code is mandatory");
            }
        } else if (resource instanceof Consent consent) {
            if (!consent.hasStatus()) {
                errors.add("Missing required field: Consent status is mandatory");
            }
            if (!consent.hasScope()) {
                errors.add("Missing required field: Consent scope is mandatory");
            }
        }
    }

    /**
     * Outcome record/class holding validation results.
     */
    public static class ValidationOutcome {
        private final boolean valid;
        private final List<String> errors;

        public ValidationOutcome(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors != null ? errors : new ArrayList<>();
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}
