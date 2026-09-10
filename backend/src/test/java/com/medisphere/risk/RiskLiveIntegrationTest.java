package com.medisphere.risk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisphere.audit.model.AuditAction;
import com.medisphere.audit.model.AuditLog;
import com.medisphere.audit.repository.AuditLogRepository;
import com.medisphere.auth.model.Role;
import com.medisphere.auth.model.User;
import com.medisphere.auth.repository.UserRepository;
import com.medisphere.consent.model.Consent;
import com.medisphere.consent.model.ConsentStatus;
import com.medisphere.consent.repository.ConsentRepository;
import com.medisphere.patient.model.Patient;
import com.medisphere.patient.repository.PatientRepository;
import com.medisphere.risk.client.MlServiceClient;
import com.medisphere.risk.dto.EncounterContextDTO;
import com.medisphere.risk.dto.FeatureAttributionDTO;
import com.medisphere.risk.dto.RiskPredictionResponseDTO;
import com.medisphere.risk.exception.MlServiceUnavailableException;
import com.medisphere.risk.model.RiskPrediction;
import com.medisphere.risk.repository.RiskPredictionRepository;
import com.medisphere.twin.model.HealthTwin;
import com.medisphere.twin.model.TwinDemographics;
import com.medisphere.twin.model.TwinLabs;
import com.medisphere.twin.model.TwinVitals;
import com.medisphere.twin.repository.HealthTwinRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@DisplayName("Live Phase 14 End-to-End Integration, Security, and Persistence Test")
class RiskLiveIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private HealthTwinRepository healthTwinRepository;

    @Autowired
    private RiskPredictionRepository riskPredictionRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConsentRepository consentRepository;

    @MockitoBean
    private MlServiceClient mlServiceClient;

    private static final String PATIENT_ID = "pat-live-cvd";
    private static final String UNASSIGNED_PATIENT_ID = "pat-live-unassigned";
    private static final String REVOKED_PATIENT_ID = "pat-live-revoked";
    private static final String INCOMPLETE_PATIENT_ID = "pat-live-incomplete";

    @BeforeEach
    void setUp() {
        cleanup();

        // 1. Seed Users
        User adminUser = new User("admin_live", "admin@medisphere.org", "hash", Role.ADMIN, null, null);
        adminUser.setActive(true);
        userRepository.save(adminUser);

        User providerUser = new User("dr_smith", "dr_smith@medisphere.org", "hash", Role.PROVIDER, null, "prov-001");
        providerUser.setActive(true);
        userRepository.save(providerUser);

        User patientUser = new User("john_doe", "john@medisphere.org", "hash", Role.PATIENT, PATIENT_ID, null);
        patientUser.setActive(true);
        userRepository.save(patientUser);

        // 2. Seed Patients
        Patient patient1 = new Patient("MRN-LIVE-1", "John", "Doe", LocalDate.of(1970, 1, 1), "Male");
        patient1.setId(PATIENT_ID);
        patient1.setAssignedProviderIds(List.of("prov-001"));
        patientRepository.save(patient1);

        Patient patient2 = new Patient("MRN-LIVE-2", "Bob", "Unassigned", LocalDate.of(1980, 5, 5), "Male");
        patient2.setId(UNASSIGNED_PATIENT_ID);
        patient2.setAssignedProviderIds(List.of("prov-999")); // assigned to other provider
        patientRepository.save(patient2);

        Patient patient3 = new Patient("MRN-LIVE-3", "Alice", "Revoked", LocalDate.of(1985, 8, 8), "Female");
        patient3.setId(REVOKED_PATIENT_ID);
        patient3.setAssignedProviderIds(List.of("prov-001"));
        patientRepository.save(patient3);

        Patient patientIncomplete = new Patient("MRN-LIVE-4", "Dave", "Incomplete", LocalDate.of(1965, 3, 3), "Male");
        patientIncomplete.setId(INCOMPLETE_PATIENT_ID);
        patientIncomplete.setAssignedProviderIds(List.of("prov-001"));
        patientRepository.save(patientIncomplete);

        // 3. Seed Consents
        Consent activeConsent = new Consent(PATIENT_ID, "prov-001", "CLINICAL_DATA", ConsentStatus.GRANTED,
                Instant.now(), Instant.now().plusSeconds(86400), "Clinical care");
        consentRepository.save(activeConsent);

        Consent revokedConsent = new Consent(REVOKED_PATIENT_ID, "prov-001", "CLINICAL_DATA", ConsentStatus.REVOKED,
                Instant.now(), Instant.now().plusSeconds(86400), "Revoked by patient");
        consentRepository.save(revokedConsent);

        // 4. Seed Complete HealthTwin for pat-live-cvd
        HealthTwin twin = new HealthTwin();
        twin.setId("twin-live-001");
        twin.setPatientId(PATIENT_ID);

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

        healthTwinRepository.save(twin);

        // 5. Seed Incomplete HealthTwin
        HealthTwin incompleteTwin = new HealthTwin();
        incompleteTwin.setId("twin-live-inc");
        incompleteTwin.setPatientId(INCOMPLETE_PATIENT_ID);
        TwinDemographics incDemo = new TwinDemographics();
        incDemo.setAge(60);
        incDemo.setGender("Male");
        incompleteTwin.setDemographics(incDemo);
        // Missing vitals and labs!
        healthTwinRepository.save(incompleteTwin);
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    private void cleanup() {
        userRepository.deleteAll();
        patientRepository.deleteAll();
        healthTwinRepository.deleteAll();
        riskPredictionRepository.deleteAll();
        auditLogRepository.deleteAll();
        consentRepository.deleteAll();
    }

    private RiskPredictionResponseDTO sampleCvdResponse() {
        List<FeatureAttributionDTO> attributions = List.of(
                new FeatureAttributionDTO("gender", 1.0, 0.3688, "INCREASES_RISK", 1),
                new FeatureAttributionDTO("age", 55.0, 0.3485, "INCREASES_RISK", 2),
                new FeatureAttributionDTO("glucose", 110.0, 0.1890, "INCREASES_RISK", 3),
                new FeatureAttributionDTO("cholesterol", 210.0, -0.0496, "DECREASES_RISK", 4),
                new FeatureAttributionDTO("diastolicBP", 85.0, 0.0287, "INCREASES_RISK", 5),
                new FeatureAttributionDTO("systolicBP", 130.0, -0.0248, "DECREASES_RISK", 6),
                new FeatureAttributionDTO("heartRate", 72.0, 0.0066, "INCREASES_RISK", 7),
                new FeatureAttributionDTO("bmi", 28.4, -0.0035, "DECREASES_RISK", 8)
        );

        return new RiskPredictionResponseDTO(
                "CARDIOVASCULAR",
                "cardiovascular_federated_logistic_regression",
                "1.0.0-phase12",
                0.6467,
                "HIGH",
                0.5,
                "log_odds",
                -0.2592,
                0.6044,
                attributions,
                "Model feature contributions reflect statistical model behavior for academic research only."
        );
    }

    private RiskPredictionResponseDTO sampleDiabetesResponse() {
        List<FeatureAttributionDTO> attributions = new ArrayList<>();
        attributions.add(new FeatureAttributionDTO("age", 55.0, 0.1534, "INCREASES_RISK", 1));
        attributions.add(new FeatureAttributionDTO("gender", 1.0, 0.0783, "INCREASES_RISK", 2));
        attributions.add(new FeatureAttributionDTO("number_diagnoses", 7, -0.0683, "DECREASES_RISK", 3));
        attributions.add(new FeatureAttributionDTO("diabetesMed", "Yes", -0.0620, "DECREASES_RISK", 4));
        attributions.add(new FeatureAttributionDTO("A1Cresult", ">7", -0.0248, "DECREASES_RISK", 5));
        attributions.add(new FeatureAttributionDTO("insulin", "Steady", 0.0230, "INCREASES_RISK", 6));
        attributions.add(new FeatureAttributionDTO("time_in_hospital", 4, -0.0114, "DECREASES_RISK", 7));
        attributions.add(new FeatureAttributionDTO("num_medications", 14, -0.0093, "DECREASES_RISK", 8));
        attributions.add(new FeatureAttributionDTO("max_glu_serum", "norm", 0.0015, "INCREASES_RISK", 9));
        attributions.add(new FeatureAttributionDTO("num_procedures", 1, -0.0008, "DECREASES_RISK", 10));
        attributions.add(new FeatureAttributionDTO("num_lab_procedures", 45, 0.0007, "INCREASES_RISK", 11));

        return new RiskPredictionResponseDTO(
                "DIABETES",
                "diabetes_complications_federated_logistic_regression",
                "1.0.0-phase12",
                0.4990,
                "MODERATE",
                0.51,
                "log_odds",
                -0.0846,
                -0.0041,
                attributions,
                "Model feature contributions reflect statistical model behavior for academic research only."
        );
    }

    private EncounterContextDTO sampleEncounterContext() {
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

    // =========================================================================
    // TEST 3: LIVE CVD END-TO-END TEST + MONGO + AUDIT
    // =========================================================================
    @Test
    @DisplayName("3. Live CVD End-to-End: 200, federated model, SHAP attributions, MongoDB persistence, Twin update, Audit")
    @WithMockUser(username = "admin_live", roles = {"ADMIN"})
    void testLiveCvdEndToEnd() throws Exception {
        when(mlServiceClient.predictAndExplain(eq("CARDIOVASCULAR"), eq("cardiovascular_federated_logistic_regression"), any()))
                .thenReturn(sampleCvdResponse());

        mockMvc.perform(post("/api/patients/" + PATIENT_ID + "/risk/cardiovascular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model_name").value("cardiovascular_federated_logistic_regression"))
                .andExpect(jsonPath("$.model_version").value("1.0.0-phase12"))
                .andExpect(jsonPath("$.model_estimated_risk_probability").value(0.6467))
                .andExpect(jsonPath("$.estimated_risk_tier").value("HIGH"))
                .andExpect(jsonPath("$.decision_threshold").value(0.5))
                .andExpect(jsonPath("$.explanation_space").value("log_odds"))
                .andExpect(jsonPath("$.feature_attributions.length()").value(8));

        // 1. Inspect MongoDB risk_predictions
        List<RiskPrediction> persistedPredictions = riskPredictionRepository.findByPatientIdOrderByEvaluatedAtDesc(PATIENT_ID);
        assertThat(persistedPredictions).hasSize(1);
        RiskPrediction pred = persistedPredictions.get(0);
        assertThat(pred.getPatientId()).isEqualTo(PATIENT_ID);
        assertThat(pred.getModelName()).isEqualTo("cardiovascular_federated_logistic_regression");
        assertThat(pred.getModelVersion()).isEqualTo("1.0.0-phase12");
        assertThat(pred.getRiskProbability()).isEqualTo(0.6467);
        assertThat(pred.getRiskTier()).isEqualTo("HIGH");
        assertThat(pred.getFeatureAttributions()).hasSize(8);

        // 2. Inspect HealthTwin.riskScores
        HealthTwin updatedTwin = healthTwinRepository.findByPatientId(PATIENT_ID).orElseThrow();
        assertThat(updatedTwin.getRiskScores()).isNotNull();
        @SuppressWarnings("unchecked")
        Map<String, Object> cardioSummary = (Map<String, Object>) updatedTwin.getRiskScores().get("cardiovascularRisk");
        assertThat(cardioSummary).isNotNull();
        assertThat(cardioSummary.get("modelEstimatedRiskProbability")).isEqualTo(0.6467);
        assertThat(cardioSummary.get("estimatedRiskTier")).isEqualTo("HIGH");
        assertThat(cardioSummary.get("modelName")).isEqualTo("cardiovascular_federated_logistic_regression");

        // 3. Inspect audit_logs for ONE ML_PREDICTION event
        List<AuditLog> auditLogs = auditLogRepository.findByPatientIdOrderByTimestampDesc(PATIENT_ID);
        long predictionEvents = auditLogs.stream()
                .filter(l -> l.getAction() == AuditAction.ML_PREDICTION)
                .count();
        assertThat(predictionEvents).isEqualTo(1);
    }

    // =========================================================================
    // TEST 4: PROVIDER SECURITY TEST (dr_smith)
    // =========================================================================
    @Test
    @DisplayName("4. Provider Security: Assigned+Consent (200), Unassigned (403), Revoked Consent (403) with ACCESS_DENIED audit")
    @WithMockUser(username = "dr_smith", roles = {"PROVIDER"})
    void testProviderSecurity() throws Exception {
        when(mlServiceClient.predictAndExplain(eq("CARDIOVASCULAR"), eq("cardiovascular_federated_logistic_regression"), any()))
                .thenReturn(sampleCvdResponse());

        // A. Assigned + Active Consent -> 200
        mockMvc.perform(post("/api/patients/" + PATIENT_ID + "/risk/cardiovascular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimated_risk_tier").value("HIGH"));

        // B. Unassigned Patient -> 403 Forbidden
        mockMvc.perform(post("/api/patients/" + UNASSIGNED_PATIENT_ID + "/risk/cardiovascular"))
                .andExpect(status().isForbidden());

        // C. Assigned Patient + Revoked Consent -> 403 Forbidden
        mockMvc.perform(post("/api/patients/" + REVOKED_PATIENT_ID + "/risk/cardiovascular"))
                .andExpect(status().isForbidden());

        // Verify ACCESS_DENIED audit entries exist
        List<AuditLog> allLogs = auditLogRepository.findAll();
        long deniedEvents = allLogs.stream()
                .filter(l -> l.getAction() == AuditAction.ACCESS_DENIED && "dr_smith".equals(l.getUsername()))
                .count();
        assertThat(deniedEvents).isGreaterThanOrEqualTo(2);
    }

    // =========================================================================
    // TEST 5: PATIENT SECURITY TEST (john_doe)
    // =========================================================================
    @Test
    @DisplayName("5. Patient Security: Own linked patient (200), other patient (403) with ACCESS_DENIED audit")
    @WithMockUser(username = "john_doe", roles = {"PATIENT"})
    void testPatientSecurity() throws Exception {
        when(mlServiceClient.predictAndExplain(eq("CARDIOVASCULAR"), eq("cardiovascular_federated_logistic_regression"), any()))
                .thenReturn(sampleCvdResponse());

        // A. Own linked patient -> 200
        mockMvc.perform(post("/api/patients/" + PATIENT_ID + "/risk/cardiovascular"))
                .andExpect(status().isOk());

        // B. Other patient record -> 403 Forbidden
        mockMvc.perform(post("/api/patients/" + UNASSIGNED_PATIENT_ID + "/risk/cardiovascular"))
                .andExpect(status().isForbidden());

        // Verify ACCESS_DENIED audit entry for john_doe
        List<AuditLog> allLogs = auditLogRepository.findAll();
        long deniedEvents = allLogs.stream()
                .filter(l -> l.getAction() == AuditAction.ACCESS_DENIED && "john_doe".equals(l.getUsername()))
                .count();
        assertThat(deniedEvents).isGreaterThanOrEqualTo(1);
    }

    // =========================================================================
    // TEST 6: DIABETES END-TO-END + MISSING DATA 422
    // =========================================================================
    @Test
    @DisplayName("6. Diabetes End-to-End: 11 features, persistence, Twin update, and 422 on missing encounter data")
    @WithMockUser(username = "admin_live", roles = {"ADMIN"})
    void testDiabetesEndToEndAndMissingData() throws Exception {
        when(mlServiceClient.predictAndExplain(eq("DIABETES"), eq("diabetes_complications_federated_logistic_regression"), any()))
                .thenReturn(sampleDiabetesResponse());

        // A. Valid encounter context -> 200 Success
        mockMvc.perform(post("/api/patients/" + PATIENT_ID + "/risk/diabetes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleEncounterContext())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.task_type").value("DIABETES"))
                .andExpect(jsonPath("$.feature_attributions.length()").value(11));

        // Confirm RiskPrediction persisted for Diabetes
        List<RiskPrediction> persisted = riskPredictionRepository.findByPatientIdOrderByEvaluatedAtDesc(PATIENT_ID);
        assertThat(persisted).hasSize(1);
        assertThat(persisted.get(0).getTaskType()).isEqualTo("DIABETES");
        assertThat(persisted.get(0).getModelName()).isEqualTo("diabetes_complications_federated_logistic_regression");

        // Confirm HealthTwin updated
        HealthTwin twin = healthTwinRepository.findByPatientId(PATIENT_ID).orElseThrow();
        assertThat(twin.getRiskScores().get("diabetesComplicationsRisk")).isNotNull();
        @SuppressWarnings("unchecked")
        Map<String, Object> diabSummary = (Map<String, Object>) twin.getRiskScores().get("diabetesComplicationsRisk");
        assertThat(diabSummary.get("modelName")).isEqualTo("diabetes_complications_federated_logistic_regression");

        // B. Incomplete encounter context -> 400 Bad Request (Jakarta Bean Validation)
        EncounterContextDTO emptyContext = new EncounterContextDTO();
        mockMvc.perform(post("/api/patients/" + PATIENT_ID + "/risk/diabetes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyContext)))
                .andExpect(status().isBadRequest());

        // C. Incomplete patient HealthTwin data -> 422 Insufficient Clinical Data
        mockMvc.perform(post("/api/patients/" + INCOMPLETE_PATIENT_ID + "/risk/cardiovascular"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"));
    }

    // =========================================================================
    // TEST 7: NO FAKE FALLBACK TEST (ML Service Down -> 503)
    // =========================================================================
    @Test
    @DisplayName("7. No Fake Fallback: Returns 503 when ML service fails; zero fake records created")
    @WithMockUser(username = "admin_live", roles = {"ADMIN"})
    void testNoFakeFallback() throws Exception {
        when(mlServiceClient.predictAndExplain(any(), any(), any()))
                .thenThrow(new MlServiceUnavailableException("Clinical risk estimation service is unreachable."));

        mockMvc.perform(post("/api/patients/" + PATIENT_ID + "/risk/cardiovascular"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("Service Unavailable"));

        // Assert NO fake predictions were saved
        List<RiskPrediction> persisted = riskPredictionRepository.findByPatientIdOrderByEvaluatedAtDesc(PATIENT_ID);
        assertThat(persisted).isEmpty();

        // Assert HealthTwin riskScores was NOT touched (either null or empty)
        HealthTwin twin = healthTwinRepository.findByPatientId(PATIENT_ID).orElseThrow();
        assertThat(twin.getRiskScores() == null || twin.getRiskScores().isEmpty()).isTrue();
    }
}
