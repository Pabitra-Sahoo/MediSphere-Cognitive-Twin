package com.medisphere.consent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisphere.audit.service.AuditService;
import com.medisphere.auth.model.Role;
import com.medisphere.auth.model.User;
import com.medisphere.auth.repository.UserRepository;
import com.medisphere.auth.security.JwtAuthFilter;
import com.medisphere.auth.security.JwtTokenProvider;
import com.medisphere.auth.security.SecurityEvaluationService;
import com.medisphere.config.SecurityConfig;
import com.medisphere.consent.dto.ConsentCreateRequest;
import com.medisphere.consent.dto.ConsentDTO;
import com.medisphere.consent.dto.ConsentVerifyResponse;
import com.medisphere.consent.model.ConsentStatus;
import com.medisphere.consent.service.ConsentService;
import com.medisphere.patient.model.Patient;
import com.medisphere.patient.repository.PatientRepository;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ConsentController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, SecurityEvaluationService.class})
class ConsentControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    @Test
    @DisplayName("GET /api/patients/{id}/consents unauthenticated returns 401 Unauthorized")
    void testGetConsentsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/patients/pat-001/consents"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/patients/{id}/consents as ADMIN returns 200 OK")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testGetConsentsAsAdmin() throws Exception {
        ConsentDTO dto = new ConsentDTO();
        dto.setId("c-1");
        dto.setPatientId("pat-001");
        dto.setGrantedTo("prov-001");
        dto.setStatus(ConsentStatus.GRANTED);

        when(consentService.getConsents("pat-001")).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/patients/pat-001/consents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("c-1"));
    }

    @Test
    @DisplayName("GET /api/patients/{id}/consents as PATIENT for own record returns 200 OK")
    @WithMockUser(username = "john_doe", roles = {"PATIENT"})
    void testGetConsentsAsPatientOwnRecord() throws Exception {
        User patientUser = new User("john_doe", "john@test.org", "hash", Role.PATIENT, "pat-001", null);
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(patientUser));

        ConsentDTO dto = new ConsentDTO();
        dto.setId("c-1");
        dto.setPatientId("pat-001");
        dto.setGrantedTo("prov-001");
        dto.setStatus(ConsentStatus.GRANTED);

        when(consentService.getConsents("pat-001")).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/patients/pat-001/consents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("c-1"));
    }

    @Test
    @DisplayName("GET /api/patients/{id}/consents as PATIENT for other patient returns 403 Forbidden")
    @WithMockUser(username = "john_doe", roles = {"PATIENT"})
    void testGetConsentsAsPatientOtherRecord() throws Exception {
        User patientUser = new User("john_doe", "john@test.org", "hash", Role.PATIENT, "pat-001", null);
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(patientUser));

        mockMvc.perform(get("/api/patients/pat-002/consents"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/patients/{id}/consents as PATIENT for own record returns 201 Created")
    @WithMockUser(username = "john_doe", roles = {"PATIENT"})
    void testGrantConsentAsPatientOwnRecord() throws Exception {
        User patientUser = new User("john_doe", "john@test.org", "hash", Role.PATIENT, "pat-001", null);
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(patientUser));

        ConsentCreateRequest req = new ConsentCreateRequest();
        req.setGrantedTo("prov-001");
        req.setScope("READ_ALL");
        req.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));

        ConsentDTO dto = new ConsentDTO();
        dto.setId("c-new");
        dto.setPatientId("pat-001");
        dto.setGrantedTo("prov-001");
        dto.setStatus(ConsentStatus.GRANTED);

        when(consentService.grantConsent(eq("pat-001"), any(ConsentCreateRequest.class), eq("john_doe")))
                .thenReturn(dto);

        mockMvc.perform(post("/api/patients/pat-001/consents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("c-new"))
                .andExpect(jsonPath("$.status").value("GRANTED"));
    }

    @Test
    @DisplayName("POST /api/patients/{id}/consents as PROVIDER returns 403 Forbidden")
    @WithMockUser(username = "dr_smith", roles = {"PROVIDER"})
    void testGrantConsentAsProviderForbidden() throws Exception {
        User providerUser = new User("dr_smith", "dr@test.org", "hash", Role.PROVIDER, null, "prov-001");
        when(userRepository.findByUsername("dr_smith")).thenReturn(Optional.of(providerUser));

        ConsentCreateRequest req = new ConsentCreateRequest();
        req.setGrantedTo("prov-001");
        req.setScope("READ_ALL");

        mockMvc.perform(post("/api/patients/pat-001/consents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/patients/{id}/consents/{consentId}/revoke as PATIENT for own record returns 200 OK")
    @WithMockUser(username = "john_doe", roles = {"PATIENT"})
    void testRevokeConsentAsPatientOwnRecord() throws Exception {
        User patientUser = new User("john_doe", "john@test.org", "hash", Role.PATIENT, "pat-001", null);
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(patientUser));

        ConsentDTO dto = new ConsentDTO();
        dto.setId("c-1");
        dto.setPatientId("pat-001");
        dto.setStatus(ConsentStatus.REVOKED);

        when(consentService.revokeConsent(eq("pat-001"), eq("c-1"), eq("john_doe"))).thenReturn(dto);

        mockMvc.perform(put("/api/patients/pat-001/consents/c-1/revoke"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVOKED"));
    }

    @Test
    @DisplayName("GET /api/patients/{id}/consents/verify as PROVIDER returns 200 OK")
    @WithMockUser(username = "dr_smith", roles = {"PROVIDER"})
    void testVerifyConsentAsProvider() throws Exception {
        User providerUser = new User("dr_smith", "dr@test.org", "hash", Role.PROVIDER, null, "prov-001");
        when(userRepository.findByUsername("dr_smith")).thenReturn(Optional.of(providerUser));

        ConsentVerifyResponse response = new ConsentVerifyResponse(true, "c-1", "READ_ALL", Instant.now().plus(10, ChronoUnit.DAYS));
        when(consentService.verifyConsent("pat-001", "prov-001")).thenReturn(response);

        mockMvc.perform(get("/api/patients/pat-001/consents/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasConsent").value(true));
    }
}
