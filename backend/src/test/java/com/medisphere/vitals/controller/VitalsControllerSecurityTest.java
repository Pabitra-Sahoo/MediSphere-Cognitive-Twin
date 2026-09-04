package com.medisphere.vitals.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisphere.auth.model.Role;
import com.medisphere.auth.model.User;
import com.medisphere.auth.repository.UserRepository;
import com.medisphere.auth.security.JwtAuthFilter;
import com.medisphere.auth.security.JwtTokenProvider;
import com.medisphere.auth.security.SecurityEvaluationService;
import com.medisphere.config.SecurityConfig;
import com.medisphere.patient.model.Patient;
import com.medisphere.patient.repository.PatientRepository;
import com.medisphere.vitals.dto.VitalsDTO;
import com.medisphere.vitals.dto.VitalsEventDTO;
import com.medisphere.vitals.dto.VitalsSimulateRequest;
import com.medisphere.vitals.service.VitalsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = VitalsController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, SecurityEvaluationService.class})
class VitalsControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VitalsService vitalsService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PatientRepository patientRepository;

    // ==========================================
    // 1. POST /api/vitals/simulate RBAC
    // ==========================================

    @Test
    @DisplayName("POST /api/vitals/simulate unauthenticated returns 401 Unauthorized")
    void testSimulateUnauthenticated() throws Exception {
        VitalsSimulateRequest req = new VitalsSimulateRequest();
        req.setPatientId("pat-001");
        req.setHeartRate(75.0);

        mockMvc.perform(post("/api/vitals/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/vitals/simulate as PATIENT returns 403 Forbidden")
    @WithMockUser(username = "john_doe", roles = {"PATIENT"})
    void testSimulateForbiddenForPatient() throws Exception {
        VitalsSimulateRequest req = new VitalsSimulateRequest();
        req.setPatientId("pat-001");
        req.setHeartRate(75.0);

        mockMvc.perform(post("/api/vitals/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/vitals/simulate as PROVIDER returns 403 Forbidden")
    @WithMockUser(username = "dr_smith", roles = {"PROVIDER"})
    void testSimulateForbiddenForProvider() throws Exception {
        VitalsSimulateRequest req = new VitalsSimulateRequest();
        req.setPatientId("pat-001");
        req.setHeartRate(75.0);

        mockMvc.perform(post("/api/vitals/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/vitals/simulate as ADMIN returns 202 Accepted")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testSimulateAllowedForAdmin() throws Exception {
        VitalsSimulateRequest req = new VitalsSimulateRequest();
        req.setPatientId("pat-001");
        req.setHeartRate(75.0);

        VitalsEventDTO event = new VitalsEventDTO();
        event.setEventId("evt-sim-1");
        event.setPatientId("pat-001");
        event.setHeartRate(75.0);

        when(vitalsService.simulateVitals(any(VitalsSimulateRequest.class))).thenReturn(event);

        mockMvc.perform(post("/api/vitals/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.eventId").value("evt-sim-1"))
                .andExpect(jsonPath("$.patientId").value("pat-001"));
    }

    // ==========================================
    // 2. GET /api/patients/{patientId}/vitals RBAC
    // ==========================================

    @Test
    @DisplayName("GET /api/patients/{id}/vitals unauthenticated returns 401 Unauthorized")
    void testGetVitalsHistoryUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/patients/pat-001/vitals"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/patients/{id}/vitals as ADMIN returns 200 OK")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testGetVitalsHistoryAllowedForAdmin() throws Exception {
        User adminUser = new User("admin", "admin@test.org", "hash", Role.ADMIN, null, null);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(vitalsService.getVitalsHistory(eq("pat-001"), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/patients/pat-001/vitals"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/patients/{id}/vitals as assigned PROVIDER returns 200 OK")
    @WithMockUser(username = "dr_smith", roles = {"PROVIDER"})
    void testGetVitalsHistoryAllowedForAssignedProvider() throws Exception {
        User providerUser = new User("dr_smith", "dr@test.org", "hash", Role.PROVIDER, null, "prov-001");
        when(userRepository.findByUsername("dr_smith")).thenReturn(Optional.of(providerUser));

        Patient patient = new Patient("MRN-1", "John", "Doe", LocalDate.of(1980, 1, 1), "Male");
        patient.setId("pat-001");
        patient.setAssignedProviderIds(List.of("prov-001"));
        when(patientRepository.findById("pat-001")).thenReturn(Optional.of(patient));

        when(vitalsService.getVitalsHistory(eq("pat-001"), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/patients/pat-001/vitals"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/patients/{id}/vitals as UNASSIGNED PROVIDER returns 403 Forbidden")
    @WithMockUser(username = "dr_smith", roles = {"PROVIDER"})
    void testGetVitalsHistoryForbiddenForUnassignedProvider() throws Exception {
        User providerUser = new User("dr_smith", "dr@test.org", "hash", Role.PROVIDER, null, "prov-001");
        when(userRepository.findByUsername("dr_smith")).thenReturn(Optional.of(providerUser));

        // Patient assigned only to prov-002
        Patient patient = new Patient("MRN-2", "Jane", "Roe", LocalDate.of(1985, 1, 1), "Female");
        patient.setId("pat-002");
        patient.setAssignedProviderIds(List.of("prov-002"));
        when(patientRepository.findById("pat-002")).thenReturn(Optional.of(patient));

        mockMvc.perform(get("/api/patients/pat-002/vitals"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/patients/{id}/vitals as PATIENT for own record returns 200 OK")
    @WithMockUser(username = "john_doe", roles = {"PATIENT"})
    void testGetVitalsHistoryAllowedForSelfPatient() throws Exception {
        User patientUser = new User("john_doe", "john@test.org", "hash", Role.PATIENT, "pat-001", null);
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(patientUser));

        when(vitalsService.getVitalsHistory(eq("pat-001"), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/patients/pat-001/vitals"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/patients/{id}/vitals as PATIENT for another patient returns 403 Forbidden")
    @WithMockUser(username = "john_doe", roles = {"PATIENT"})
    void testGetVitalsHistoryForbiddenForOtherPatient() throws Exception {
        // User john_doe has linkedPatientId pat-001, attempting to access pat-002
        User patientUser = new User("john_doe", "john@test.org", "hash", Role.PATIENT, "pat-001", null);
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(patientUser));

        mockMvc.perform(get("/api/patients/pat-002/vitals"))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // 3. GET /api/patients/{patientId}/vitals/latest RBAC
    // ==========================================

    @Test
    @DisplayName("GET /api/patients/{id}/vitals/latest unauthenticated returns 401 Unauthorized")
    void testGetLatestVitalsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/patients/pat-001/vitals/latest"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/patients/{id}/vitals/latest as ADMIN returns 200 OK")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testGetLatestVitalsAllowedForAdmin() throws Exception {
        User adminUser = new User("admin", "admin@test.org", "hash", Role.ADMIN, null, null);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        VitalsDTO dto = new VitalsDTO();
        dto.setPatientId("pat-001");
        dto.setHeartRate(72.0);
        when(vitalsService.getLatestVitals("pat-001")).thenReturn(dto);

        mockMvc.perform(get("/api/patients/pat-001/vitals/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value("pat-001"))
                .andExpect(jsonPath("$.heartRate").value(72.0));
    }
}
