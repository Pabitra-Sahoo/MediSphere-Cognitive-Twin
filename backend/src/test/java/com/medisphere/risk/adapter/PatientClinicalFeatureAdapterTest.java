package com.medisphere.risk.adapter;

import com.medisphere.risk.dto.EncounterContextDTO;
import com.medisphere.risk.exception.InsufficientClinicalDataException;
import com.medisphere.twin.model.HealthTwin;
import com.medisphere.twin.model.TwinDemographics;
import com.medisphere.twin.model.TwinLabs;
import com.medisphere.twin.model.TwinVitals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PatientClinicalFeatureAdapterTest {

    private PatientClinicalFeatureAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PatientClinicalFeatureAdapter();
    }

    private HealthTwin createCompleteTwin() {
        HealthTwin twin = new HealthTwin();
        twin.setId("twin-001");
        twin.setPatientId("pat-001");

        TwinDemographics demo = new TwinDemographics();
        demo.setAge(55);
        demo.setGender("Male");
        demo.setBmi(28.4);
        twin.setDemographics(demo);

        TwinVitals vitals = new TwinVitals();
        vitals.setSystolicBP(130.0);
        vitals.setDiastolicBP(85.0);
        vitals.setHeartRate(72.0);
        twin.setLatestVitals(vitals);

        TwinLabs labs = new TwinLabs();
        labs.setGlucose(110.0);
        labs.setCholesterol(210.0);
        twin.setLatestLabs(labs);

        return twin;
    }

    private EncounterContextDTO createCompleteEncounterContext() {
        EncounterContextDTO ctx = new EncounterContextDTO();
        ctx.setTimeInHospital(4);
        ctx.setNumLabProcedures(45);
        ctx.setNumProcedures(1);
        ctx.setNumMedications(14);
        ctx.setNumberDiagnoses(7);
        ctx.setMaxGluSerum("norm");
        ctx.setA1cResult(">7");
        ctx.setInsulin("Steady");
        ctx.setDiabetesMed("Yes");
        return ctx;
    }

    @Test
    @DisplayName("extractCardiovascularFeatures: extracts exactly 8 features from complete HealthTwin")
    void testExtractCardiovascularFeaturesSuccess() {
        HealthTwin twin = createCompleteTwin();
        Map<String, Object> features = adapter.extractCardiovascularFeatures(twin);

        assertNotNull(features);
        assertEquals(8, features.size());
        assertEquals(55.0, features.get("age"));
        assertEquals(1.0, features.get("gender"));
        assertEquals(28.4, features.get("bmi"));
        assertEquals(130.0, features.get("systolicBP"));
        assertEquals(85.0, features.get("diastolicBP"));
        assertEquals(72.0, features.get("heartRate"));
        assertEquals(110.0, features.get("glucose"));
        assertEquals(210.0, features.get("cholesterol"));

        // Confirm absolutely no PHI
        assertFalse(features.containsKey("patientId"));
        assertFalse(features.containsKey("id"));
    }

    @Test
    @DisplayName("extractCardiovascularFeatures: calculates BMI from height and weight if BMI null")
    void testExtractCardiovascularFeaturesBmiCalculated() {
        HealthTwin twin = createCompleteTwin();
        twin.getDemographics().setBmi(null);
        twin.getDemographics().setHeight(175.0); // 1.75m
        twin.getDemographics().setWeight(70.0); // 70 / (1.75^2) = 22.857

        Map<String, Object> features = adapter.extractCardiovascularFeatures(twin);
        double bmi = (Double) features.get("bmi");
        assertEquals(22.86, bmi, 0.05);
    }

    @Test
    @DisplayName("extractCardiovascularFeatures: throws 422 InsufficientClinicalDataException on missing vitals/labs")
    void testExtractCardiovascularFeaturesMissingData() {
        HealthTwin twin = createCompleteTwin();
        twin.getLatestVitals().setSystolicBP(null);
        twin.getLatestLabs().setCholesterol(null);

        InsufficientClinicalDataException ex = assertThrows(
                InsufficientClinicalDataException.class,
                () -> adapter.extractCardiovascularFeatures(twin)
        );

        assertTrue(ex.getMissingFields().contains("systolicBP"));
        assertTrue(ex.getMissingFields().contains("cholesterol"));
    }

    @Test
    @DisplayName("extractDiabetesFeatures: extracts 11 features from twin and encounter context")
    void testExtractDiabetesFeaturesSuccess() {
        HealthTwin twin = createCompleteTwin();
        EncounterContextDTO ctx = createCompleteEncounterContext();

        Map<String, Object> features = adapter.extractDiabetesFeatures(twin, ctx);

        assertNotNull(features);
        assertEquals(11, features.size());
        assertEquals(55.0, features.get("age"));
        assertEquals(1.0, features.get("gender"));
        assertEquals(4, features.get("time_in_hospital"));
        assertEquals(45, features.get("num_lab_procedures"));
        assertEquals(1, features.get("num_procedures"));
        assertEquals(14, features.get("num_medications"));
        assertEquals(7, features.get("number_diagnoses"));
        assertEquals("norm", features.get("max_glu_serum"));
        assertEquals(">7", features.get("A1Cresult"));
        assertEquals("Steady", features.get("insulin"));
        assertEquals("Yes", features.get("diabetesMed"));

        // Confirm de-identification
        assertFalse(features.containsKey("patientId"));
    }

    @Test
    @DisplayName("extractDiabetesFeatures: throws 422 InsufficientClinicalDataException if encounter context missing or incomplete")
    void testExtractDiabetesFeaturesMissingEncounterData() {
        HealthTwin twin = createCompleteTwin();

        // Null context
        InsufficientClinicalDataException ex1 = assertThrows(
                InsufficientClinicalDataException.class,
                () -> adapter.extractDiabetesFeatures(twin, null)
        );
        assertEquals(9, ex1.getMissingFields().size());

        // Incomplete context
        EncounterContextDTO ctx = createCompleteEncounterContext();
        ctx.setInsulin(null);
        ctx.setTimeInHospital(null);

        InsufficientClinicalDataException ex2 = assertThrows(
                InsufficientClinicalDataException.class,
                () -> adapter.extractDiabetesFeatures(twin, ctx)
        );
        assertTrue(ex2.getMissingFields().contains("insulin"));
        assertTrue(ex2.getMissingFields().contains("timeInHospital"));
    }
}
