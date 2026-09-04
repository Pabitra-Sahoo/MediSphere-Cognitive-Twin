package com.medisphere.audit.service;

import com.medisphere.audit.dto.AuditLogDTO;
import com.medisphere.audit.model.AuditAction;
import com.medisphere.audit.model.AuditLog;
import com.medisphere.audit.model.AuditOutcome;
import com.medisphere.audit.repository.AuditLogRepository;
import com.medisphere.auth.model.Role;
import com.medisphere.auth.model.User;
import com.medisphere.auth.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private AuditService auditService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setSecurityUser(String username, Role role) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    @Test
    @DisplayName("log with SecurityContext populates user info and saves audit log")
    void testLogWithSecurityContext() {
        setSecurityUser("dr_smith", Role.PROVIDER);

        User user = new User("dr_smith", "dr@test.org", "hash", Role.PROVIDER, null, "prov-001");
        user.setId("u-prov-1");
        when(userRepository.findByUsername("dr_smith")).thenReturn(Optional.of(user));

        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        auditService.log(AuditAction.VIEW_PATIENT, "PATIENT", "pat-001", "pat-001", "Accessed demographics", AuditOutcome.SUCCESS);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getAction()).isEqualTo(AuditAction.VIEW_PATIENT);
        assertThat(saved.getOutcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(saved.getResourceType()).isEqualTo("PATIENT");
        assertThat(saved.getResourceId()).isEqualTo("pat-001");
        assertThat(saved.getPatientId()).isEqualTo("pat-001");
        assertThat(saved.getUserId()).isEqualTo("u-prov-1");
        assertThat(saved.getUsername()).isEqualTo("dr_smith");
        assertThat(saved.getUserRole()).isEqualTo("PROVIDER");
        assertThat(saved.getDetails()).isEqualTo("Accessed demographics");
        assertThat(saved.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("direct log saves explicit userId, role, and ipAddress")
    void testDirectLog() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        auditService.log("u-1", "dr_smith", "PROVIDER", AuditAction.ACCESS_DENIED,
                "PATIENT", "pat-003", "pat-003", "Provider not assigned", AuditOutcome.DENIED, "192.168.1.100");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getAction()).isEqualTo(AuditAction.ACCESS_DENIED);
        assertThat(saved.getOutcome()).isEqualTo(AuditOutcome.DENIED);
        assertThat(saved.getUserId()).isEqualTo("u-1");
        assertThat(saved.getUsername()).isEqualTo("dr_smith");
        assertThat(saved.getUserRole()).isEqualTo("PROVIDER");
        assertThat(saved.getPatientId()).isEqualTo("pat-003");
        assertThat(saved.getDetails()).isEqualTo("Provider not assigned");
        assertThat(saved.getIpAddress()).isEqualTo("192.168.1.100");
    }

    @Test
    @DisplayName("sanitizes Bearer tokens and credentials from details")
    void testSanitizesSensitiveData() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        auditService.log("u-1", "dr_smith", "PROVIDER", AuditAction.VIEW_PATIENT,
                "PATIENT", "pat-001", "pat-001", "Header Authorization: Bearer eyJhbGciOiJIUzI1Ni.secret.signature and password=supersecret",
                AuditOutcome.SUCCESS, "127.0.0.1");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getDetails()).doesNotContain("eyJhbGciOiJIUzI1Ni");
        assertThat(saved.getDetails()).doesNotContain("supersecret");
        assertThat(saved.getDetails()).contains("[REDACTED]");
    }

    @Test
    @DisplayName("getPatientAuditLogs queries mongoTemplate and returns paged DTOs")
    void testGetPatientAuditLogs() {
        AuditLog log1 = new AuditLog("u-1", "dr_smith", "PROVIDER", AuditAction.VIEW_TWIN,
                "TWIN", "twin-001", "pat-001", "Viewed twin", AuditOutcome.SUCCESS, "127.0.0.1", Instant.now());
        log1.setId("log-1");

        when(mongoTemplate.count(any(Query.class), eq(AuditLog.class))).thenReturn(1L);
        when(mongoTemplate.find(any(Query.class), eq(AuditLog.class))).thenReturn(List.of(log1));

        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditLogDTO> result = auditService.getPatientAuditLogs("pat-001", AuditAction.VIEW_TWIN, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo("log-1");
        assertThat(result.getContent().get(0).getAction()).isEqualTo(AuditAction.VIEW_TWIN);
    }
}
