package com.medisphere.risk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisphere.audit.service.AuditService;
import com.medisphere.auth.model.Role;
import com.medisphere.auth.model.User;
import com.medisphere.auth.repository.UserRepository;
import com.medisphere.auth.security.JwtAuthFilter;
import com.medisphere.auth.security.JwtTokenProvider;
import com.medisphere.auth.security.SecurityEvaluationService;
import com.medisphere.config.SecurityConfig;
import com.medisphere.consent.service.ConsentService;
import com.medisphere.patient.model.Patient;
import com.medisphere.patient.repository.PatientRepository;
import com.medisphere.risk.client.MlServiceClient;
import com.medisphere.risk.dto.EncounterContextDTO;
import com.medisphere.risk.dto.FeatureAttributionDTO;
import com.medisphere.risk.dto.RiskPredictionResponseDTO;
import com.medisphere.risk.service.RiskAssessmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {RiskAssessmentController.class, MlModelCatalogController.class})
@Import({SecurityConfig.class, JwtAuthFilter.class, SecurityEvaluationService.class})
class RiskAssessmentControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RiskAssessmentService riskAssessmentService;

    @MockitoBean
    private MlServiceClient mlServiceClient;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PatientRepository patientRepository;

    @MockitoBean
    private ConsentService consentService;

    @MockitoBean
    private AuditService auditService;

    private RiskPredictionResponseDTO sampleResponse() {
        return new RiskPredictionResponseDTO(
                "cardiovascular_risk",
                "cardiovascular_federated_logistic_regression",
                "federated_flwr_fedavg",
                0.35,
                "MODERATE",
                0.5,
                "log_odds",
                -0.619,
                -0.650,
                List.of(new FeatureAttributionDTO("age", 55.0, 0.25, "INCREASES_RISK", 1)),
                "test-safety-disclaimer"
        );
    }

    private EncounterContextDTO sampleEncounterContext() {
        EncounterContextDTO ctx = new EncounterContextDTO();
        ctx.setTimeInHospital(3);
        ctx.setNumLabProcedures(40);
        ctx.setNumProcedures(1);
        ctx.setNumMedications(12);
        ctx.setNumberDiagnoses(6);
        ctx.setMaxGluSerum("norm");
        ctx.setA1cResult(">7");
        ctx.setInsulin("Steady");
        ctx.setDiabetesMed("Yes");
        return ctx;
    }

    // ==========================================
    // 1. Unauthenticated checks
    // ==========================================

    @Test
    @DisplayName("POST /api/patients/{patientId}/risk/cardiovascular without auth returns 401")
    void testCardiovascularUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/patients/pat-001/risk/cardiovascular"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/ml/models without auth returns 401")
    void testCatalogUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/ml/models"))
                .andExpect(status().isUnauthorized());
    }

    // ==========================================
    // 2. ADMIN role checks
    // ==========================================

    @Test
    @DisplayName("POST /api/patients/{patientId}/risk/cardiovascular as ADMIN succeeds with 200")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void testCardiovascularAdminAccess() throws Exception {
        User adminUser = new User("admin_user", "admin@test.org", "hash", Role.ADMIN, null, null);
        when(userRepository.findByUsername("admin_user")).thenReturn(Optional.of(adminUser));
        when(riskAssessmentService.evaluateCardiovascularRisk(eq("pat-001"), any()))
                .thenReturn(sampleResponse());

        mockMvc.perform(post("/api/patients/pat-001/risk/cardiovascular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimated_risk_tier").value("MODERATE"));
    }

    @Test
    @DisplayName("GET /api/ml/models as ADMIN succeeds with 200")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void testCatalogAdminAccess() throws Exception {
        when(mlServiceClient.getModelCatalog()).thenReturn(List.of());

        mockMvc.perform(get("/api/ml/models"))
                .andExpect(status().isOk());
    }

    // ==========================================
    // 3. PATIENT role checks (Self vs Cross-Access)
    // ==========================================

    @Test
    @DisplayName("POST /api/patients/{patientId}/risk/cardiovascular as PATIENT for self succeeds with 200")
    @WithMockUser(username = "patient_self", roles = {"PATIENT"})
    void testCardiovascularPatientSelfAccess() throws Exception {
        User patientUser = new User("patient_self", "patient@test.org", "hash", Role.PATIENT, "pat-001", null);
        when(userRepository.findByUsername("patient_self")).thenReturn(Optional.of(patientUser));
        when(riskAssessmentService.evaluateCardiovascularRisk(eq("pat-001"), any()))
                .thenReturn(sampleResponse());

        mockMvc.perform(post("/api/patients/pat-001/risk/cardiovascular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimated_risk_tier").value("MODERATE"));
    }

    @Test
    @DisplayName("POST /api/patients/{patientId}/risk/cardiovascular as PATIENT for another patient returns 403")
    @WithMockUser(username = "patient_self", roles = {"PATIENT"})
    void testCardiovascularPatientCrossAccessForbidden() throws Exception {
        User patientUser = new User("patient_self", "patient@test.org", "hash", Role.PATIENT, "pat-001", null);
        when(userRepository.findByUsername("patient_self")).thenReturn(Optional.of(patientUser));

        // Attempting to evaluate risk for pat-999
        mockMvc.perform(post("/api/patients/pat-999/risk/cardiovascular"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/ml/models as PATIENT returns 403 Forbidden")
    @WithMockUser(username = "patient_self", roles = {"PATIENT"})
    void testCatalogForbiddenForPatient() throws Exception {
        mockMvc.perform(get("/api/ml/models"))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // 4. PROVIDER role checks (Assigned + Consent vs Unassigned / No Consent)
    // ==========================================

    @Test
    @DisplayName("POST /api/patients/{patientId}/risk/cardiovascular as PROVIDER with assignment and consent succeeds")
    @WithMockUser(username = "dr_smith", roles = {"PROVIDER"})
    void testCardiovascularProviderWithConsentAllowed() throws Exception {
        User providerUser = new User("dr_smith", "dr@test.org", "hash", Role.PROVIDER, null, "prov-001");
        when(userRepository.findByUsername("dr_smith")).thenReturn(Optional.of(providerUser));

        Patient patient = new Patient("MRN-1", "John", "Doe", LocalDate.of(1980, 1, 1), "Male");
        patient.setId("pat-001");
        patient.setAssignedProviderIds(List.of("prov-001"));
        when(patientRepository.findById("pat-001")).thenReturn(Optional.of(patient));
        when(consentService.hasActiveConsent("pat-001", "prov-001")).thenReturn(true);

        when(riskAssessmentService.evaluateCardiovascularRisk(eq("pat-001"), any()))
                .thenReturn(sampleResponse());

        mockMvc.perform(post("/api/patients/pat-001/risk/cardiovascular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimated_risk_tier").value("MODERATE"));
    }

    @Test
    @DisplayName("POST /api/patients/{patientId}/risk/cardiovascular as unassigned PROVIDER returns 403")
    @WithMockUser(username = "dr_smith", roles = {"PROVIDER"})
    void testCardiovascularProviderUnassignedForbidden() throws Exception {
        User providerUser = new User("dr_smith", "dr@test.org", "hash", Role.PROVIDER, null, "prov-001");
        when(userRepository.findByUsername("dr_smith")).thenReturn(Optional.of(providerUser));

        // Patient assigned to someone else
        Patient patient = new Patient("MRN-2", "Jane", "Doe", LocalDate.of(1985, 1, 1), "Female");
        patient.setId("pat-002");
        patient.setAssignedProviderIds(List.of("prov-999"));
        when(patientRepository.findById("pat-002")).thenReturn(Optional.of(patient));

        mockMvc.perform(post("/api/patients/pat-002/risk/cardiovascular"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/patients/{patientId}/risk/cardiovascular as PROVIDER without active consent returns 403")
    @WithMockUser(username = "dr_smith", roles = {"PROVIDER"})
    void testCardiovascularProviderWithoutConsentForbidden() throws Exception {
        User providerUser = new User("dr_smith", "dr@test.org", "hash", Role.PROVIDER, null, "prov-001");
        when(userRepository.findByUsername("dr_smith")).thenReturn(Optional.of(providerUser));

        Patient patient = new Patient("MRN-1", "John", "Doe", LocalDate.of(1980, 1, 1), "Male");
        patient.setId("pat-001");
        patient.setAssignedProviderIds(List.of("prov-001"));
        when(patientRepository.findById("pat-001")).thenReturn(Optional.of(patient));
        when(consentService.hasActiveConsent("pat-001", "prov-001")).thenReturn(false); // Revoked/missing

        mockMvc.perform(post("/api/patients/pat-001/risk/cardiovascular"))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // 5. Diabetes endpoint validation
    // ==========================================

    @Test
    @DisplayName("POST /api/patients/{patientId}/risk/diabetes as ADMIN with valid encounter returns 200")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void testDiabetesAdminSuccess() throws Exception {
        User adminUser = new User("admin_user", "admin@test.org", "hash", Role.ADMIN, null, null);
        when(userRepository.findByUsername("admin_user")).thenReturn(Optional.of(adminUser));
        when(riskAssessmentService.evaluateDiabetesRisk(eq("pat-001"), any(), any()))
                .thenReturn(sampleResponse());

        mockMvc.perform(post("/api/patients/pat-001/risk/diabetes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleEncounterContext())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimated_risk_tier").value("MODERATE"));
    }

    @Test
    @DisplayName("POST /api/patients/{patientId}/risk/diabetes with missing required encounter fields returns 400 Bad Request")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void testDiabetesMissingFieldsBadRequest() throws Exception {
        EncounterContextDTO badCtx = new EncounterContextDTO();
        // missing timeInHospital, insulin, etc.

        mockMvc.perform(post("/api/patients/pat-001/risk/diabetes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badCtx)))
                .andExpect(status().isBadRequest());
    }
}
