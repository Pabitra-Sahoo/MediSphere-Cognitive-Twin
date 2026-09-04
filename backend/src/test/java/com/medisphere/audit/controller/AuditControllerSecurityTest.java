package com.medisphere.audit.controller;

import com.medisphere.audit.dto.AuditLogDTO;
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
import com.medisphere.consent.service.ConsentService;
import com.medisphere.patient.repository.PatientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuditController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, SecurityEvaluationService.class, com.medisphere.common.exception.GlobalExceptionHandler.class})
class AuditControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditService auditService;

    @MockitoBean
    private ConsentService consentService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PatientRepository patientRepository;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("GET /api/audit-logs unauthenticated returns 401 Unauthorized")
    void testGetAuditLogsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/audit-logs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/audit-logs as PROVIDER returns 403 Forbidden")
    @WithMockUser(username = "dr_smith", roles = {"PROVIDER"})
    void testGetAuditLogsAsProviderForbidden() throws Exception {
        mockMvc.perform(get("/api/audit-logs"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/audit-logs as PATIENT returns 403 Forbidden")
    @WithMockUser(username = "john_doe", roles = {"PATIENT"})
    void testGetAuditLogsAsPatientForbidden() throws Exception {
        mockMvc.perform(get("/api/audit-logs"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/audit-logs as ADMIN returns 200 OK")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testGetAuditLogsAsAdmin() throws Exception {
        com.medisphere.audit.model.AuditLog log = new com.medisphere.audit.model.AuditLog(
                "u-1", "dr_smith", "PROVIDER", AuditAction.VIEW_TWIN,
                "TWIN", "twin-001", "pat-001", "Details", AuditOutcome.SUCCESS, "127.0.0.1", Instant.now());
        log.setId("log-1");
        AuditLogDTO dto = new AuditLogDTO(log);

        when(auditService.getAllAuditLogs(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/api/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("log-1"));
    }

    @Test
    @DisplayName("GET /api/patients/{id}/audit-logs as PATIENT for own record returns 200 OK")
    @WithMockUser(username = "john_doe", roles = {"PATIENT"})
    void testGetPatientAuditLogsAsPatientOwnRecord() throws Exception {
        User patientUser = new User("john_doe", "john@test.org", "hash", Role.PATIENT, "pat-001", null);
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(patientUser));

        com.medisphere.audit.model.AuditLog log = new com.medisphere.audit.model.AuditLog(
                "u-1", "dr_smith", "PROVIDER", AuditAction.VIEW_TWIN,
                "TWIN", "twin-001", "pat-001", "Details", AuditOutcome.SUCCESS, "127.0.0.1", Instant.now());
        log.setId("log-1");
        AuditLogDTO dto = new AuditLogDTO(log);

        when(auditService.getPatientAuditLogs(eq("pat-001"), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/api/patients/pat-001/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].patientId").value("pat-001"));
    }

    @Test
    @DisplayName("GET /api/patients/{id}/audit-logs as PATIENT for other patient returns 403 Forbidden")
    @WithMockUser(username = "john_doe", roles = {"PATIENT"})
    void testGetPatientAuditLogsAsPatientOtherRecord() throws Exception {
        User patientUser = new User("john_doe", "john@test.org", "hash", Role.PATIENT, "pat-001", null);
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(patientUser));

        mockMvc.perform(get("/api/patients/pat-002/audit-logs"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Mutation endpoints (POST/PUT/DELETE) on /api/audit-logs do not exist (405)")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testAuditLogImmutabilityNoMutations() throws Exception {
        mockMvc.perform(post("/api/audit-logs"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(put("/api/audit-logs"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(delete("/api/audit-logs"))
                .andExpect(status().isMethodNotAllowed());
    }
}
