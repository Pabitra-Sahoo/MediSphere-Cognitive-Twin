package com.medisphere.fhir;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisphere.auth.model.Role;
import com.medisphere.auth.model.User;
import com.medisphere.auth.repository.UserRepository;
import com.medisphere.auth.security.JwtAuthFilter;
import com.medisphere.auth.security.JwtTokenProvider;
import com.medisphere.auth.security.SecurityEvaluationService;
import com.medisphere.config.SecurityConfig;
import com.medisphere.fhir.controller.FhirController;
import com.medisphere.fhir.dto.FhirIngestionResult;
import com.medisphere.fhir.model.FhirResource;
import com.medisphere.fhir.repository.FhirResourceRepository;
import com.medisphere.fhir.service.FhirIngestionService;
import com.medisphere.patient.model.Patient;
import com.medisphere.patient.repository.PatientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {FhirController.class})
@Import({SecurityConfig.class, JwtAuthFilter.class, SecurityEvaluationService.class})
class FhirControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FhirIngestionService fhirIngestionService;

    @MockitoBean
    private FhirResourceRepository fhirResourceRepository;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PatientRepository patientRepository;

    @Test
    @DisplayName("POST /api/fhir/ingest without authentication returns 401 Unauthorized")
    void testIngestUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/fhir/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/fhir/ingest as PATIENT returns 403 Forbidden")
    @WithMockUser(username = "john_doe", roles = {"PATIENT"})
    void testIngestAsPatientForbidden() throws Exception {
        mockMvc.perform(post("/api/fhir/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/fhir/ingest as PROVIDER returns 403 Forbidden")
    @WithMockUser(username = "dr_smith", roles = {"PROVIDER"})
    void testIngestAsProviderForbidden() throws Exception {
        mockMvc.perform(post("/api/fhir/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/fhir/ingest as ADMIN returns 200 OK")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testIngestAsAdminOk() throws Exception {
        FhirIngestionResult mockResult = new FhirIngestionResult(1, 1, 0, List.of());
        when(fhirIngestionService.ingest(any())).thenReturn(mockResult);

        mockMvc.perform(post("/api/fhir/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceType\": \"Patient\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/patients/{patientId}/fhir-resources unauthenticated returns 401")
    void testGetPatientFhirResourcesUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/patients/pat-001/fhir-resources"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/patients/{patientId}/fhir-resources as PROVIDER for assigned patient returns 200 OK")
    @WithMockUser(username = "dr_smith", roles = {"PROVIDER"})
    void testGetPatientFhirResourcesAsProviderAssigned() throws Exception {
        User providerUser = new User("dr_smith", "dr@test.org", "hash", Role.PROVIDER, null, "prov-001");
        when(userRepository.findByUsername("dr_smith")).thenReturn(Optional.of(providerUser));

        Patient patient = new Patient("MRN-10001", "John", "Doe", null, "Male");
        patient.setId("pat-001");
        patient.setAssignedProviderIds(List.of("prov-001"));
        when(patientRepository.findById("pat-001")).thenReturn(Optional.of(patient));
        when(fhirResourceRepository.findByPatientId(eq("pat-001"), any(Sort.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/patients/pat-001/fhir-resources"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/patients/{patientId}/fhir-resources as PROVIDER for unassigned patient returns 403 Forbidden")
    @WithMockUser(username = "dr_smith", roles = {"PROVIDER"})
    void testGetPatientFhirResourcesAsProviderUnassigned() throws Exception {
        User providerUser = new User("dr_smith", "dr@test.org", "hash", Role.PROVIDER, null, "prov-001");
        when(userRepository.findByUsername("dr_smith")).thenReturn(Optional.of(providerUser));

        Patient unassignedPatient = new Patient("MRN-10003", "Robert", "Chen", null, "Male");
        unassignedPatient.setId("pat-003");
        unassignedPatient.setAssignedProviderIds(List.of("prov-999")); // not prov-001
        when(patientRepository.findById("pat-003")).thenReturn(Optional.of(unassignedPatient));

        mockMvc.perform(get("/api/patients/pat-003/fhir-resources"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/patients/{patientId}/fhir-resources as PATIENT for own record returns 200 OK")
    @WithMockUser(username = "john_doe", roles = {"PATIENT"})
    void testGetPatientFhirResourcesAsPatientOwnRecord() throws Exception {
        User patientUser = new User("john_doe", "john@test.org", "hash", Role.PATIENT, "pat-001", null);
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(patientUser));
        when(fhirResourceRepository.findByPatientId(eq("pat-001"), any(Sort.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/patients/pat-001/fhir-resources"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/patients/{patientId}/fhir-resources as PATIENT for another patient returns 403 Forbidden")
    @WithMockUser(username = "john_doe", roles = {"PATIENT"})
    void testGetPatientFhirResourcesAsPatientOtherRecord() throws Exception {
        User patientUser = new User("john_doe", "john@test.org", "hash", Role.PATIENT, "pat-001", null);
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(patientUser));

        mockMvc.perform(get("/api/patients/pat-002/fhir-resources"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/fhir/resources/{resourceId} unauthenticated returns 401")
    void testGetFhirResourceUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/fhir/resources/obs-001"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/fhir/resources/{resourceId} as ADMIN returns 200 OK")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testGetFhirResourceAsAdmin() throws Exception {
        User adminUser = new User("admin", "admin@test.org", "hash", Role.ADMIN, null, null);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        FhirResource resource = new FhirResource("pat-001", "Observation", "obs-001", "{}", "VALID", List.of());
        when(fhirResourceRepository.findByResourceId("obs-001")).thenReturn(Optional.of(resource));

        mockMvc.perform(get("/api/fhir/resources/obs-001"))
                .andExpect(status().isOk());
    }
}
