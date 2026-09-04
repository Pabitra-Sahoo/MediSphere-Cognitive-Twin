package com.medisphere.consent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisphere.audit.model.AuditAction;
import com.medisphere.audit.model.AuditOutcome;
import com.medisphere.audit.service.AuditService;
import com.medisphere.auth.model.Role;
import com.medisphere.auth.model.User;
import com.medisphere.auth.repository.UserRepository;
import com.medisphere.auth.security.JwtAuthFilter;
import com.medisphere.auth.security.JwtTokenProvider;
import com.medisphere.auth.security.SecurityEvaluationService;
import com.medisphere.config.SecurityConfig;
import com.medisphere.consent.controller.ConsentController;
import com.medisphere.consent.service.ConsentService;
import com.medisphere.fhir.controller.FhirController;
import com.medisphere.fhir.repository.FhirResourceRepository;
import com.medisphere.fhir.service.FhirIngestionService;
import com.medisphere.lab.controller.LabController;
import com.medisphere.lab.repository.LabResultRepository;
import com.medisphere.patient.controller.PatientController;
import com.medisphere.patient.dto.PatientDTO;
import com.medisphere.patient.model.Patient;
import com.medisphere.patient.repository.PatientRepository;
import com.medisphere.patient.service.PatientService;
import com.medisphere.twin.controller.HealthTwinController;
import com.medisphere.twin.dto.HealthTwinDTO;
import com.medisphere.twin.dto.TwinCompletenessDTO;
import com.medisphere.twin.service.HealthTwinService;
import com.medisphere.vitals.controller.VitalsController;
import com.medisphere.vitals.dto.VitalsDTO;
import com.medisphere.vitals.service.VitalsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        PatientController.class,
        HealthTwinController.class,
        VitalsController.class,
        LabController.class,
        FhirController.class,
        ConsentController.class
})
@Import({SecurityConfig.class, JwtAuthFilter.class, SecurityEvaluationService.class})
class ConsentEnforcementSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PatientService patientService;

    @MockitoBean
    private HealthTwinService healthTwinService;

    @MockitoBean
    private VitalsService vitalsService;

    @MockitoBean
    private LabResultRepository labResultRepository;

    @MockitoBean
    private FhirIngestionService fhirIngestionService;

    @MockitoBean
    private FhirResourceRepository fhirResourceRepository;

    @MockitoBean
    private ConsentService consentService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PatientRepository patientRepository;

    @MockitoBean
    private AuditService auditService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private User providerUser;
    private Patient patient1;

    @BeforeEach
    void setUp() {
        providerUser = new User("dr_smith", "dr@test.org", "hash", Role.PROVIDER, null, "prov-001");
        providerUser.setId("u-prov-1");
        when(userRepository.findByUsername("dr_smith")).thenReturn(Optional.of(providerUser));

        User adminUser = new User("admin", "admin@test.org", "hash", Role.ADMIN, null, null);
        adminUser.setId("u-admin-1");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        patient1 = new Patient("MRN-10001", "John", "Doe", LocalDate.of(1980, 1, 1), "Male");
        patient1.setId("pat-001");
        patient1.setAssignedProviderIds(List.of("prov-001"));
        when(patientRepository.findById("pat-001")).thenReturn(Optional.of(patient1));

        // Mock default DTO returns
        when(patientService.getPatientById("pat-001")).thenReturn(new PatientDTO());
        HealthTwinDTO twinDTO = new HealthTwinDTO();
        twinDTO.setCompleteness(new TwinCompletenessDTO(100.0, List.of(), 20, 20, Instant.now()));
        when(healthTwinService.getTwinByPatientId("pat-001")).thenReturn(twinDTO);
        when(vitalsService.getVitalsHistory(eq("pat-001"), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(vitalsService.getLatestVitals("pat-001")).thenReturn(new VitalsDTO());
        when(labResultRepository.findByPatientId(eq("pat-001"), any())).thenReturn(new PageImpl<>(List.of()));
        when(fhirResourceRepository.findByPatientId(eq("pat-001"), any(Sort.class))).thenReturn(List.of());
    }

    // =========================================================================
    // 1. PROVIDER WITH ACTIVE CONSENT -> 200 OK ON ALL 6 PROTECTED ENDPOINTS
    // =========================================================================

    @Test
    @DisplayName("Provider with assignment and active consent can access all 6 protected endpoints")
    @WithMockUser(username = "dr_smith", roles = {"PROVIDER"})
    void testProviderWithConsentAllowed() throws Exception {
        when(consentService.hasActiveConsent("pat-001", "prov-001")).thenReturn(true);

        mockMvc.perform(get("/api/patients/pat-001")).andExpect(status().isOk());
        mockMvc.perform(get("/api/patients/pat-001/twin")).andExpect(status().isOk());
        mockMvc.perform(get("/api/patients/pat-001/vitals")).andExpect(status().isOk());
        mockMvc.perform(get("/api/patients/pat-001/vitals/latest")).andExpect(status().isOk());
        mockMvc.perform(get("/api/patients/pat-001/labs")).andExpect(status().isOk());
        mockMvc.perform(get("/api/patients/pat-001/fhir-resources")).andExpect(status().isOk());
    }

    // =========================================================================
    // 2. PROVIDER WITHOUT CONSENT -> 403 FORBIDDEN AND DIRECT ACCESS_DENIED AUDIT
    // =========================================================================

    @Test
    @DisplayName("Provider with assignment but NO active consent is denied (403) and directly audited")
    @WithMockUser(username = "dr_smith", roles = {"PROVIDER"})
    void testProviderWithoutConsentDenied() throws Exception {
        when(consentService.hasActiveConsent("pat-001", "prov-001")).thenReturn(false);

        // Access twin without consent -> 403
        mockMvc.perform(get("/api/patients/pat-001/twin"))
                .andExpect(status().isForbidden());

        // Verify SecurityEvaluationService directly audited the ACCESS_DENIED event
        verify(auditService).log(
                eq("u-prov-1"),
                eq("dr_smith"),
                eq("PROVIDER"),
                eq(AuditAction.ACCESS_DENIED),
                eq("PATIENT"),
                eq("pat-001"),
                eq("pat-001"),
                contains("no active consent found for provider prov-001"),
                eq(AuditOutcome.DENIED),
                any()
        );
    }

    @Test
    @DisplayName("Provider without consent is denied on all 6 endpoints")
    @WithMockUser(username = "dr_smith", roles = {"PROVIDER"})
    void testProviderWithoutConsentDeniedOnAllEndpoints() throws Exception {
        when(consentService.hasActiveConsent("pat-001", "prov-001")).thenReturn(false);

        mockMvc.perform(get("/api/patients/pat-001")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/patients/pat-001/twin")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/patients/pat-001/vitals")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/patients/pat-001/vitals/latest")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/patients/pat-001/labs")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/patients/pat-001/fhir-resources")).andExpect(status().isForbidden());
    }

    // =========================================================================
    // 3. TWIN COMPLETENESS ACCESSIBLE WITHOUT CONSENT
    // =========================================================================

    @Test
    @DisplayName("GET /api/patients/{id}/twin/completeness remains accessible to assigned provider WITHOUT consent")
    @WithMockUser(username = "dr_smith", roles = {"PROVIDER"})
    void testTwinCompletenessAccessibleWithoutConsent() throws Exception {
        // Explicitly set hasActiveConsent to false
        when(consentService.hasActiveConsent("pat-001", "prov-001")).thenReturn(false);

        TwinCompletenessDTO completeness = new TwinCompletenessDTO(96.0, List.of(), 20, 20, Instant.now());
        when(healthTwinService.getCompletenessByPatientId("pat-001")).thenReturn(completeness);

        mockMvc.perform(get("/api/patients/pat-001/twin/completeness"))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // 4. ADMIN HAS GLOBAL ACCESS WITHOUT CONSENT
    // =========================================================================

    @Test
    @DisplayName("ADMIN has access to protected endpoints without consent")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testAdminAccessWithoutConsent() throws Exception {
        mockMvc.perform(get("/api/patients/pat-001")).andExpect(status().isOk());
        mockMvc.perform(get("/api/patients/pat-001/twin")).andExpect(status().isOk());
        mockMvc.perform(get("/api/patients/pat-001/vitals")).andExpect(status().isOk());
    }

    // =========================================================================
    // 5. PATIENT OWN RECORD ACCESS WITHOUT CONSENT
    // =========================================================================

    @Test
    @DisplayName("PATIENT can access own records without consent")
    @WithMockUser(username = "john_doe", roles = {"PATIENT"})
    void testPatientOwnRecordAccessWithoutConsent() throws Exception {
        User patientUser = new User("john_doe", "john@test.org", "hash", Role.PATIENT, "pat-001", null);
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(patientUser));

        mockMvc.perform(get("/api/patients/pat-001")).andExpect(status().isOk());
        mockMvc.perform(get("/api/patients/pat-001/twin")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATIENT accessing another patient record is denied (403) and audited")
    @WithMockUser(username = "john_doe", roles = {"PATIENT"})
    void testPatientOtherRecordAccessDenied() throws Exception {
        User patientUser = new User("john_doe", "john@test.org", "hash", Role.PATIENT, "pat-001", null);
        patientUser.setId("u-patient-1");
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(patientUser));

        mockMvc.perform(get("/api/patients/pat-002/twin"))
                .andExpect(status().isForbidden());

        verify(auditService).log(
                eq("u-patient-1"),
                eq("john_doe"),
                eq("PATIENT"),
                eq(AuditAction.ACCESS_DENIED),
                eq("PATIENT"),
                eq("pat-002"),
                eq("pat-002"),
                contains("patient attempted to access unauthorized patient record"),
                eq(AuditOutcome.DENIED),
                any()
        );
    }
}
