package com.medisphere.risk.adapter;

import com.medisphere.risk.dto.EncounterContextDTO;
import com.medisphere.risk.exception.InsufficientClinicalDataException;
import com.medisphere.twin.model.HealthTwin;
import com.medisphere.twin.model.TwinDemographics;
import com.medisphere.twin.model.TwinLabs;
import com.medisphere.twin.model.TwinVitals;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps and validates clinical features extracted from a patient's HealthTwin and clinical encounters.
 * Enforces the strict rule: NEVER fabricate or substitute missing clinical measurements.
 * Completely strips all patient identifiers (no patientId, name, MRN, phone, address).
 */
@Component
public class PatientClinicalFeatureAdapter {

    /**
     * Constructs the 8-feature vector required for Cardiovascular risk estimation.
     * All 8 features are sourced 100% from the patient's current HealthTwin.
     *
     * @param twin Patient's current digital health twin
     * @return Ordered de-identified map of 8 numeric features
     * @throws InsufficientClinicalDataException if any of the 8 required features is missing
     */
    public Map<String, Object> extractCardiovascularFeatures(HealthTwin twin) {
        if (twin == null) {
            throw new InsufficientClinicalDataException(
                    "HealthTwin record is missing for patient",
                    List.of("age", "gender", "bmi", "systolicBP", "diastolicBP", "heartRate", "glucose", "cholesterol")
            );
        }

        List<String> missing = new ArrayList<>();
        TwinDemographics demo = twin.getDemographics();
        TwinVitals vitals = twin.getLatestVitals();
        TwinLabs labs = twin.getLatestLabs();

        // 1. Age
        Double age = null;
        if (demo != null && demo.getAge() != null) {
            age = demo.getAge().doubleValue();
        } else {
            missing.add("age");
        }

        // 2. Gender (1.0 = Male, 0.0 = Female)
        Double gender = null;
        if (demo != null && demo.getGender() != null) {
            String g = demo.getGender().trim().toLowerCase();
            if (g.startsWith("m") || "1".equals(g) || "1.0".equals(g)) {
                gender = 1.0;
            } else if (g.startsWith("f") || "0".equals(g) || "0.0".equals(g)) {
                gender = 0.0;
            } else {
                gender = 0.0;
            }
        } else {
            missing.add("gender");
        }

        // 3. BMI (use explicit BMI, or compute from height/weight if both available)
        Double bmi = null;
        if (demo != null && demo.getBmi() != null) {
            bmi = demo.getBmi();
        } else if (demo != null && demo.getHeight() != null && demo.getWeight() != null && demo.getHeight() > 0) {
            double heightMeters = demo.getHeight() > 3.0 ? demo.getHeight() / 100.0 : demo.getHeight();
            bmi = demo.getWeight() / (heightMeters * heightMeters);
        } else {
            missing.add("bmi");
        }

        // 4. Systolic Blood Pressure
        Double systolicBP = null;
        if (vitals != null && vitals.getSystolicBP() != null) {
            systolicBP = vitals.getSystolicBP();
        } else {
            missing.add("systolicBP");
        }

        // 5. Diastolic Blood Pressure
        Double diastolicBP = null;
        if (vitals != null && vitals.getDiastolicBP() != null) {
            diastolicBP = vitals.getDiastolicBP();
        } else {
            missing.add("diastolicBP");
        }

        // 6. Heart Rate
        Double heartRate = null;
        if (vitals != null && vitals.getHeartRate() != null) {
            heartRate = vitals.getHeartRate();
        } else {
            missing.add("heartRate");
        }

        // 7. Glucose
        Double glucose = null;
        if (labs != null && labs.getGlucose() != null) {
            glucose = labs.getGlucose();
        } else {
            missing.add("glucose");
        }

        // 8. Cholesterol
        Double cholesterol = null;
        if (labs != null && labs.getCholesterol() != null) {
            cholesterol = labs.getCholesterol();
        } else {
            missing.add("cholesterol");
        }

        if (!missing.isEmpty()) {
            throw new InsufficientClinicalDataException(
                    "Missing required clinical features for cardiovascular risk evaluation: " + missing,
                    missing
            );
        }

        Map<String, Object> features = new LinkedHashMap<>();
        features.put("age", age);
        features.put("gender", gender);
        features.put("bmi", bmi);
        features.put("systolicBP", systolicBP);
        features.put("diastolicBP", diastolicBP);
        features.put("heartRate", heartRate);
        features.put("glucose", glucose);
        features.put("cholesterol", cholesterol);

        return features;
    }

    /**
     * Constructs the 11-feature vector required for Diabetes Complications risk estimation.
     * Demographics come from the HealthTwin; acute encounter parameters MUST come from EncounterContextDTO.
     *
     * @param twin Patient's current digital health twin
     * @param context Explicit clinical encounter context
     * @return Ordered de-identified map of 11 features
     * @throws InsufficientClinicalDataException if any required feature is missing
     */
    public Map<String, Object> extractDiabetesFeatures(HealthTwin twin, EncounterContextDTO context) {
        List<String> missing = new ArrayList<>();

        if (twin == null) {
            missing.add("age");
            missing.add("gender");
        }

        Double age = null;
        Double gender = null;
        if (twin != null && twin.getDemographics() != null) {
            TwinDemographics demo = twin.getDemographics();
            if (demo.getAge() != null) {
                age = demo.getAge().doubleValue();
            } else {
                missing.add("age");
            }

            if (demo.getGender() != null) {
                String g = demo.getGender().trim().toLowerCase();
                gender = (g.startsWith("m") || "1".equals(g) || "1.0".equals(g)) ? 1.0 : 0.0;
            } else {
                missing.add("gender");
            }
        } else if (twin != null) {
            missing.add("age");
            missing.add("gender");
        }

        if (context == null) {
            missing.addAll(List.of(
                    "timeInHospital", "numLabProcedures", "numProcedures", "numMedications",
                    "numberDiagnoses", "maxGluSerum", "a1cResult", "insulin", "diabetesMed"
            ));
        } else {
            if (context.getTimeInHospital() == null) missing.add("timeInHospital");
            if (context.getNumLabProcedures() == null) missing.add("numLabProcedures");
            if (context.getNumProcedures() == null) missing.add("numProcedures");
            if (context.getNumMedications() == null) missing.add("numMedications");
            if (context.getNumberDiagnoses() == null) missing.add("numberDiagnoses");
            if (context.getMaxGluSerum() == null || context.getMaxGluSerum().isBlank()) missing.add("maxGluSerum");
            if (context.getA1cResult() == null || context.getA1cResult().isBlank()) missing.add("a1cResult");
            if (context.getInsulin() == null || context.getInsulin().isBlank()) missing.add("insulin");
            if (context.getDiabetesMed() == null || context.getDiabetesMed().isBlank()) missing.add("diabetesMed");
        }

        if (!missing.isEmpty()) {
            throw new InsufficientClinicalDataException(
                    "Missing required clinical encounter features for diabetes complications evaluation: " + missing,
                    missing
            );
        }

        Map<String, Object> features = new LinkedHashMap<>();
        features.put("age", age);
        features.put("gender", gender);
        features.put("time_in_hospital", context.getTimeInHospital());
        features.put("num_lab_procedures", context.getNumLabProcedures());
        features.put("num_procedures", context.getNumProcedures());
        features.put("num_medications", context.getNumMedications());
        features.put("number_diagnoses", context.getNumberDiagnoses());
        features.put("max_glu_serum", context.getMaxGluSerum());
        features.put("A1Cresult", context.getA1cResult());
        features.put("insulin", context.getInsulin());
        features.put("diabetesMed", context.getDiabetesMed());

        return features;
    }
}
