package com.medisphere.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import com.medisphere.config.FhirConfig;
import com.medisphere.fhir.service.FhirValidationService;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Quantity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("FhirValidationService Tests")
class FhirValidationServiceTest {

    private FhirValidationService validationService;

    @BeforeEach
    void setUp() {
        FhirConfig config = new FhirConfig();
        FhirContext fhirContext = config.fhirContext();
        FhirValidator fhirValidator = config.fhirValidator(fhirContext);
        validationService = new FhirValidationService(fhirContext, fhirValidator);
    }

    @Test
    @DisplayName("Valid Patient with name and identifier passes validation")
    void testValidPatient() {
        Patient patient = new Patient();
        patient.setId("pat-001");
        patient.addIdentifier(new Identifier().setSystem("http://medisphere.com/mrn").setValue("MRN-10001"));
        patient.addName(new HumanName().setFamily("Doe").addGiven("John"));
        patient.setBirthDate(new Date(90, 0, 1));
        patient.setGender(org.hl7.fhir.r4.model.Enumerations.AdministrativeGender.MALE);

        FhirValidationService.ValidationOutcome outcome = validationService.validate(patient);
        assertTrue(outcome.isValid(), "Valid patient should pass validation");
        assertTrue(outcome.getErrors().isEmpty());
    }

    @Test
    @DisplayName("Valid Observation with status, code, and valueQuantity passes validation")
    void testValidObservation() {
        Observation obs = new Observation();
        obs.setId("obs-001");
        obs.setStatus(Observation.ObservationStatus.FINAL);
        obs.setCode(new CodeableConcept().addCoding(
                new Coding("http://loinc.org", "8867-4", "Heart rate")));
        obs.setValue(new Quantity().setValue(72).setUnit("beats/minute"));

        FhirValidationService.ValidationOutcome outcome = validationService.validate(obs);
        assertTrue(outcome.isValid(), "Valid observation should pass validation");
    }

    @Test
    @DisplayName("Valid Bundle containing valid resources passes validation")
    void testValidBundle() {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        Patient patient = new Patient();
        patient.addName(new HumanName().setFamily("Roe").addGiven("Jane"));
        bundle.addEntry().setResource(patient);

        Observation obs = new Observation();
        obs.setStatus(Observation.ObservationStatus.FINAL);
        obs.setCode(new CodeableConcept().addCoding(new Coding("http://loinc.org", "2345-7", "Glucose")));
        obs.setValue(new Quantity().setValue(95).setUnit("mg/dL"));
        bundle.addEntry().setResource(obs);

        FhirValidationService.ValidationOutcome outcome = validationService.validate(bundle);
        assertTrue(outcome.isValid(), "Valid bundle should pass validation");
    }

    @Test
    @DisplayName("Invalid Patient missing both name and identifier fails validation")
    void testInvalidPatientMissingNameAndIdentifier() {
        Patient patient = new Patient();
        patient.setId("pat-no-name");

        FhirValidationService.ValidationOutcome outcome = validationService.validate(patient);
        assertFalse(outcome.isValid(), "Patient without name or identifier should fail validation");
        assertTrue(outcome.getErrors().stream().anyMatch(e -> e.contains("must have at least one name or identifier")));
    }

    @Test
    @DisplayName("Invalid Observation missing status fails validation")
    void testInvalidObservationMissingStatus() {
        Observation obs = new Observation();
        obs.setCode(new CodeableConcept().addCoding(new Coding("http://loinc.org", "8867-4", "Heart rate")));

        FhirValidationService.ValidationOutcome outcome = validationService.validate(obs);
        assertFalse(outcome.isValid(), "Observation missing status should fail validation");
        assertTrue(outcome.getErrors().stream().anyMatch(e -> e.contains("Observation status is mandatory")));
    }

    @Test
    @DisplayName("Invalid Observation missing code fails validation")
    void testInvalidObservationMissingCode() {
        Observation obs = new Observation();
        obs.setStatus(Observation.ObservationStatus.FINAL);

        FhirValidationService.ValidationOutcome outcome = validationService.validate(obs);
        assertFalse(outcome.isValid(), "Observation missing code should fail validation");
        assertTrue(outcome.getErrors().stream().anyMatch(e -> e.contains("Observation code")));
    }

    @Test
    @DisplayName("Null resource fails validation")
    void testNullResource() {
        FhirValidationService.ValidationOutcome outcome = validationService.validate(null);
        assertFalse(outcome.isValid());
        assertTrue(outcome.getErrors().contains("Resource is null"));
    }
}
