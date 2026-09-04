package com.medisphere.vitals.validation;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates streaming vital signs against M1 demo and data-quality boundaries.
 *
 * <p>NOTE: Boundaries implemented here are strictly data-quality ingestion sanity checks
 * and telemetry plausibility limits for the M1 demonstration system. They are NOT clinical
 * diagnosis thresholds or medical classification rules.</p>
 */
@Component
public class VitalsValidator {

    public static final double HR_MIN = 30.0;
    public static final double HR_MAX = 220.0;

    public static final double SBP_MIN = 60.0;
    public static final double SBP_MAX = 250.0;

    public static final double DBP_MIN = 30.0;
    public static final double DBP_MAX = 150.0;

    public static final double SPO2_MIN = 70.0;
    public static final double SPO2_MAX = 100.0;

    public static final double TEMP_MIN = 32.0;
    public static final double TEMP_MAX = 42.0;

    public static final double RR_MIN = 5.0;
    public static final double RR_MAX = 60.0;

    /**
     * Validates an individual measurement packet.
     * Partial readings (subsets of metrics) are accepted as long as present values fall
     * within boundaries and at least one metric is provided.
     */
    public VitalsValidationResult validate(Double heartRate,
                                          Double systolicBP,
                                          Double diastolicBP,
                                          Double oxygenSaturation,
                                          Double temperature,
                                          Double respiratoryRate) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // 1. Guard against empty measurement packets
        if (heartRate == null && systolicBP == null && diastolicBP == null
                && oxygenSaturation == null && temperature == null && respiratoryRate == null) {
            errors.add("Measurement packet is empty: At least one vital sign reading must be present");
            return VitalsValidationResult.failure(errors);
        }

        // 2. Individual data-quality plausibility boundary checks
        if (heartRate != null) {
            if (heartRate < HR_MIN || heartRate > HR_MAX) {
                errors.add(String.format("Heart rate %.1f bpm is outside data-quality boundary [%.1f - %.1f bpm]",
                        heartRate, HR_MIN, HR_MAX));
            }
        }

        if (systolicBP != null) {
            if (systolicBP < SBP_MIN || systolicBP > SBP_MAX) {
                errors.add(String.format("Systolic blood pressure %.1f mmHg is outside data-quality boundary [%.1f - %.1f mmHg]",
                        systolicBP, SBP_MIN, SBP_MAX));
            }
        }

        if (diastolicBP != null) {
            if (diastolicBP < DBP_MIN || diastolicBP > DBP_MAX) {
                errors.add(String.format("Diastolic blood pressure %.1f mmHg is outside data-quality boundary [%.1f - %.1f mmHg]",
                        diastolicBP, DBP_MIN, DBP_MAX));
            }
        }

        if (oxygenSaturation != null) {
            if (oxygenSaturation < SPO2_MIN || oxygenSaturation > SPO2_MAX) {
                errors.add(String.format("Oxygen saturation %.1f%% is outside data-quality boundary [%.1f - %.1f%%]",
                        oxygenSaturation, SPO2_MIN, SPO2_MAX));
            }
        }

        if (temperature != null) {
            if (temperature < TEMP_MIN || temperature > TEMP_MAX) {
                errors.add(String.format("Temperature %.1f °C is outside data-quality boundary [%.1f - %.1f °C]",
                        temperature, TEMP_MIN, TEMP_MAX));
            }
        }

        if (respiratoryRate != null) {
            if (respiratoryRate < RR_MIN || respiratoryRate > RR_MAX) {
                errors.add(String.format("Respiratory rate %.1f /min is outside data-quality boundary [%.1f - %.1f /min]",
                        respiratoryRate, RR_MIN, RR_MAX));
            }
        }

        // 3. Relational validation between systolic and diastolic pressure
        if (systolicBP != null && diastolicBP != null) {
            if (systolicBP <= diastolicBP) {
                errors.add(String.format("Systolic blood pressure (%.1f mmHg) must be strictly greater than diastolic blood pressure (%.1f mmHg)",
                        systolicBP, diastolicBP));
            }
        }

        if (!errors.isEmpty()) {
            return VitalsValidationResult.failure(errors, warnings);
        }

        return VitalsValidationResult.success();
    }
}
