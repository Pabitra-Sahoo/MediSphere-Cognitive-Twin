package com.medisphere.patient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisphere.auth.model.Role;
import com.medisphere.auth.model.User;
import com.medisphere.auth.repository.UserRepository;
import com.medisphere.auth.security.JwtAuthFilter;
import com.medisphere.auth.security.JwtTokenProvider;
import com.medisphere.auth.security.SecurityEvaluationService;
import com.medisphere.common.dto.PagedResponse;
import com.medisphere.config.SecurityConfig;
import com.medisphere.patient.controller.PatientController;
import com.medisphere.patient.dto.PatientDTO;
import com.medisphere.patient.dto.PatientSummaryDTO;
import com.medisphere.patient.model.Patient;
import com.medisphere.patient.repository.PatientRepository;
import com.medisphere.patient.service.PatientService;
import com.medisphere.twin.controller.HealthTwinController;
import com.medisphere.twin.dto.HealthTwinDTO;
import com.medisphere.twin.dto.TwinCompletenessDTO;
import com.medisphere.twin.service.HealthTwinService;
import com.medisphere.audit.service.AuditService;
import com.medisphere.consent.service.ConsentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {PatientController.class, HealthTwinController.class})
@Import({SecurityConfig.class, JwtAuthFilter.class, SecurityEvaluationService.class})
class PatientControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PatientService patientService;

    @MockitoBean
    private HealthTwinService healthTwinService;

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

    @Test
    @DisplayName("GET /api/patients without authentication returns 401 Unauthorized")
    void testGetPatientsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/patients as PATIENT returns 403 Forbidden")
    @WithMockUser(username = "john_doe", roles = {"PATIENT"})
    void testGetPatientsForbiddenForPatient() throws Exception {
        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/patients as PROVIDER returns 200 and assigned patient list")
    @WithMockUser(username = "dr_smith", roles = {"PROVIDER"})
    void testGetPatientsAllowedForProvider() throws Exception {
        PatientSummaryDTO summary = new PatientSummaryDTO("pat-001", "MRN-1", "John", "Doe",
                LocalDate.of(1980, 1, 1), "Male", 100.0);
        PagedResponse<PatientSummaryDTO> paged = new PagedResponse<>(List.of(summary), 0, 20, 1, 1);

        when(patientService.getPatients(eq("dr_smith"), any(), any())).thenReturn(paged);

        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("pat-001"))
                .andExpect(jsonPath("$.content[0].twinCompleteness").value(100.0));
    }

    @Test
    @DisplayName("GET /api/patients/{id} allowed for assigned PROVIDER")
    @WithMockUser(username = "dr_smith", roles = {"PROVIDER"})
    void testGetPatientAllowedForAssignedProvider() throws Exception {
        User providerUser = new User("dr_smith", "dr@test.org", "hash", Role.PROVIDER, null, "prov-001");
        when(userRepository.findByUsername("dr_smith")).thenReturn(Optional.of(providerUser));

        Patient patient = new Patient("MRN-1", "John", "Doe", LocalDate.of(1980, 1, 1), "Male");
        patient.setId("pat-001");
        patient.setAssignedProviderIds(List.of("prov-001"));
        when(patientRepository.findById("pat-001")).thenReturn(Optional.of(patient));
        when(consentService.hasActiveConsent("pat-001", "prov-001")).thenReturn(true);

        PatientDTO dto = new PatientDTO();
        dto.setId("pat-001");
        dto.setFirstName("John");
        when(patientService.getPatientById("pat-001")).thenReturn(dto);

        mockMvc.perform(get("/api/patients/pat-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("pat-001"));
    }

    @Test
    @DisplayName("GET /api/patients/{id} returns 403 Forbidden for UNASSIGNED PROVIDER")
    @WithMockUser(username = "dr_smith", roles = {"PROVIDER"})
    void testGetPatientForbiddenForUnassignedProvider() throws Exception {
        User providerUser = new User("dr_smith", "dr@test.org", "hash", Role.PROVIDER, null, "prov-001");
        when(userRepository.findByUsername("dr_smith")).thenReturn(Optional.of(providerUser));

        // Patient assigned only to prov-002 (Dr. Lee), NOT prov-001 (dr_smith)
        Patient patient = new Patient("MRN-3", "Robert", "Chen", LocalDate.of(1975, 1, 1), "Male");
        patient.setId("pat-003");
        patient.setAssignedProviderIds(List.of("prov-002"));
        when(patientRepository.findById("pat-003")).thenReturn(Optional.of(patient));

        mockMvc.perform(get("/api/patients/pat-003"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/patients/{id} allowed for PATIENT accessing own record")
    @WithMockUser(username = "john_doe", roles = {"PATIENT"})
    void testGetPatientAllowedForOwnRecord() throws Exception {
        User patientUser = new User("john_doe", "john@test.org", "hash", Role.PATIENT, "pat-001", null);
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(patientUser));

        PatientDTO dto = new PatientDTO();
        dto.setId("pat-001");
        dto.setFirstName("John");
        when(patientService.getPatientById("pat-001")).thenReturn(dto);

        mockMvc.perform(get("/api/patients/pat-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("pat-001"));
    }

    @Test
    @DisplayName("GET /api/patients/{id} returns 403 Forbidden for PATIENT accessing another patient")
    @WithMockUser(username = "john_doe", roles = {"PATIENT"})
    void testGetPatientForbiddenForOtherRecord() throws Exception {
        // User john_doe has linkedPatientId pat-001
        User patientUser = new User("john_doe", "john@test.org", "hash", Role.PATIENT, "pat-001", null);
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(patientUser));

        // Attempting to access pat-002
        mockMvc.perform(get("/api/patients/pat-002"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/patients/{id}/twin allowed for assigned PROVIDER")
    @WithMockUser(username = "dr_smith", roles = {"PROVIDER"})
    void testGetTwinAllowedForAssignedProvider() throws Exception {
        User providerUser = new User("dr_smith", "dr@test.org", "hash", Role.PROVIDER, null, "prov-001");
        when(userRepository.findByUsername("dr_smith")).thenReturn(Optional.of(providerUser));

        Patient patient = new Patient("MRN-1", "John", "Doe", LocalDate.of(1980, 1, 1), "Male");
        patient.setId("pat-001");
        patient.setAssignedProviderIds(List.of("prov-001"));
        when(patientRepository.findById("pat-001")).thenReturn(Optional.of(patient));
        when(consentService.hasActiveConsent("pat-001", "prov-001")).thenReturn(true);

        HealthTwinDTO twinDTO = new HealthTwinDTO();
        twinDTO.setId("twin-001");
        twinDTO.setPatientId("pat-001");
        twinDTO.setCompleteness(new TwinCompletenessDTO(100.0, List.of(), 20, 20, Instant.now()));
        when(healthTwinService.getTwinByPatientId("pat-001")).thenReturn(twinDTO);

        mockMvc.perform(get("/api/patients/pat-001/twin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value("pat-001"))
                .andExpect(jsonPath("$.completeness.percentage").value(100.0));
    }

    @Test
    @DisplayName("GET /api/patients/{id}/twin/completeness returns 403 for UNASSIGNED PROVIDER")
    @WithMockUser(username = "dr_smith", roles = {"PROVIDER"})
    void testGetTwinCompletenessForbiddenForUnassignedProvider() throws Exception {
        User providerUser = new User("dr_smith", "dr@test.org", "hash", Role.PROVIDER, null, "prov-001");
        when(userRepository.findByUsername("dr_smith")).thenReturn(Optional.of(providerUser));

        Patient patient = new Patient("MRN-3", "Robert", "Chen", LocalDate.of(1975, 1, 1), "Male");
        patient.setId("pat-003");
        patient.setAssignedProviderIds(List.of("prov-002")); // Unassigned to dr_smith
        when(patientRepository.findById("pat-003")).thenReturn(Optional.of(patient));

        mockMvc.perform(get("/api/patients/pat-003/twin/completeness"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/patients as PROVIDER returns 403 Forbidden")
    @WithMockUser(username = "dr_smith", roles = {"PROVIDER"})
    void testCreatePatientForbiddenForProvider() throws Exception {
        PatientDTO dto = new PatientDTO();
        dto.setMrn("MRN-NEW");
        dto.setFirstName("New");
        dto.setLastName("Patient");
        dto.setDateOfBirth(LocalDate.of(1990, 1, 1));
        dto.setGender("Male");

        mockMvc.perform(post("/api/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/patients as ADMIN returns 201 Created")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testCreatePatientAllowedForAdmin() throws Exception {
        PatientDTO dto = new PatientDTO();
        dto.setMrn("MRN-NEW");
        dto.setFirstName("New");
        dto.setLastName("Patient");
        dto.setDateOfBirth(LocalDate.of(1990, 1, 1));
        dto.setGender("Male");

        PatientDTO created = new PatientDTO();
        created.setId("pat-generated-id");
        created.setMrn("MRN-NEW");
        created.setFirstName("New");
        created.setLastName("Patient");
        created.setDateOfBirth(LocalDate.of(1990, 1, 1));
        created.setGender("Male");

        when(patientService.createPatient(any(PatientDTO.class))).thenReturn(created);

        mockMvc.perform(post("/api/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("pat-generated-id"))
                .andExpect(jsonPath("$.mrn").value("MRN-NEW"));
    }
}
