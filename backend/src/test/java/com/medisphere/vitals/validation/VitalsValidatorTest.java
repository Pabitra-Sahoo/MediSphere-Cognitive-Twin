package com.medisphere.vitals.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class VitalsValidatorTest {

    private VitalsValidator validator;

    @BeforeEach
    void setUp() {
        validator = new VitalsValidator();
    }

    @Test
    @DisplayName("Normal vital signs within boundaries return valid result")
    void testNormalVitalsValid() {
        VitalsValidationResult result = validator.validate(
                72.0,   // HR
                120.0,  // SBP
                80.0,   // DBP
                98.0,   // SpO2
                36.8,   // Temp
                16.0    // RR
        );

        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    @DisplayName("Exact boundary values return valid result")
    void testExactBoundariesValid() {
        // Lower boundaries
        VitalsValidationResult lower = validator.validate(30.0, 60.0, 30.0, 70.0, 32.0, 5.0);
        // Note: SBP 60 > DBP 30
        assertTrue(lower.isValid());

        // Upper boundaries
        VitalsValidationResult upper = validator.validate(220.0, 250.0, 150.0, 100.0, 42.0, 60.0);
        assertTrue(upper.isValid());
    }

    @Test
    @DisplayName("Empty measurement packet returns failure")
    void testEmptyPacketFails() {
        VitalsValidationResult result = validator.validate(null, null, null, null, null, null);
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("empty"));
    }

    @Test
    @DisplayName("Partial readings with valid subset of metrics are accepted")
    void testPartialReadingsValid() {
        // Only Heart Rate and Oxygen Saturation
        VitalsValidationResult result1 = validator.validate(80.0, null, null, 99.0, null, null);
        assertTrue(result1.isValid());

        // Only Blood Pressure
        VitalsValidationResult result2 = validator.validate(null, 130.0, 85.0, null, null, null);
        assertTrue(result2.isValid());

        // Only Temperature
        VitalsValidationResult result3 = validator.validate(null, null, null, null, 37.0, null);
        assertTrue(result3.isValid());
    }

    @ParameterizedTest(name = "Heart rate {0} should be invalid")
    @CsvSource({"29.9", "220.1", "0.0", "300.0"})
    @DisplayName("Heart rate outside data-quality boundary fails")
    void testHeartRateOutOfBounds(double hr) {
        VitalsValidationResult result = validator.validate(hr, 120.0, 80.0, 98.0, 36.5, 16.0);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Heart rate")));
    }

    @ParameterizedTest(name = "Systolic BP {0} should be invalid")
    @CsvSource({"59.9", "250.1", "10.0", "300.0"})
    @DisplayName("Systolic BP outside data-quality boundary fails")
    void testSystolicBpOutOfBounds(double sbp) {
        VitalsValidationResult result = validator.validate(75.0, sbp, 80.0, 98.0, 36.5, 16.0);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Systolic blood pressure")));
    }

    @ParameterizedTest(name = "Diastolic BP {0} should be invalid")
    @CsvSource({"29.9", "150.1", "10.0", "180.0"})
    @DisplayName("Diastolic BP outside data-quality boundary fails")
    void testDiastolicBpOutOfBounds(double dbp) {
        VitalsValidationResult result = validator.validate(75.0, 120.0, dbp, 98.0, 36.5, 16.0);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Diastolic blood pressure")));
    }

    @ParameterizedTest(name = "Oxygen saturation {0} should be invalid")
    @CsvSource({"69.9", "100.1", "50.0", "105.0"})
    @DisplayName("Oxygen saturation outside data-quality boundary fails")
    void testOxygenSaturationOutOfBounds(double spo2) {
        VitalsValidationResult result = validator.validate(75.0, 120.0, 80.0, spo2, 36.5, 16.0);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Oxygen saturation")));
    }

    @ParameterizedTest(name = "Temperature {0} should be invalid")
    @CsvSource({"31.9", "42.1", "25.0", "45.0"})
    @DisplayName("Temperature outside data-quality boundary fails")
    void testTemperatureOutOfBounds(double temp) {
        VitalsValidationResult result = validator.validate(75.0, 120.0, 80.0, 98.0, temp, 16.0);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Temperature")));
    }

    @ParameterizedTest(name = "Respiratory rate {0} should be invalid")
    @CsvSource({"4.9", "60.1", "1.0", "80.0"})
    @DisplayName("Respiratory rate outside data-quality boundary fails")
    void testRespiratoryRateOutOfBounds(double rr) {
        VitalsValidationResult result = validator.validate(75.0, 120.0, 80.0, 98.0, 36.5, rr);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Respiratory rate")));
    }

    @Test
    @DisplayName("Systolic BP less than or equal to diastolic BP fails relational check")
    void testRelationalBpCheck() {
        // Equal BP
        VitalsValidationResult equalResult = validator.validate(72.0, 80.0, 80.0, 98.0, 36.8, 16.0);
        assertFalse(equalResult.isValid());
        assertTrue(equalResult.getErrors().stream().anyMatch(e -> e.contains("strictly greater than diastolic")));

        // Inverted BP (systolic < diastolic)
        VitalsValidationResult invertedResult = validator.validate(72.0, 75.0, 90.0, 98.0, 36.8, 16.0);
        assertFalse(invertedResult.isValid());
        assertTrue(invertedResult.getErrors().stream().anyMatch(e -> e.contains("strictly greater than diastolic")));
    }

    @Test
    @DisplayName("Validation error messages use data-quality phrasing without clinical diagnostic terminology")
    void testNonDiagnosticPhrasing() {
        VitalsValidationResult result = validator.validate(250.0, 40.0, 100.0, 60.0, 45.0, 70.0);
        assertFalse(result.isValid());

        for (String err : result.getErrors()) {
            assertFalse(err.toLowerCase().contains("diagnosis"), "Error message should not mention 'diagnosis'");
            assertFalse(err.toLowerCase().contains("tachycardia"), "Error message should not assert 'tachycardia'");
            assertFalse(err.toLowerCase().contains("hypertension"), "Error message should not assert 'hypertension'");
            assertFalse(err.toLowerCase().contains("hypoxia"), "Error message should not assert 'hypoxia'");
        }
    }
}
