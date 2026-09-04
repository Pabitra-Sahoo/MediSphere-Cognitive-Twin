package com.medisphere.consent.service;

import com.medisphere.audit.model.AuditAction;
import com.medisphere.audit.model.AuditOutcome;
import com.medisphere.audit.service.AuditService;
import com.medisphere.auth.model.Role;
import com.medisphere.auth.model.User;
import com.medisphere.auth.repository.UserRepository;
import com.medisphere.common.exception.ResourceNotFoundException;
import com.medisphere.consent.dto.ConsentCreateRequest;
import com.medisphere.consent.dto.ConsentDTO;
import com.medisphere.consent.dto.ConsentVerifyResponse;
import com.medisphere.consent.model.Consent;
import com.medisphere.consent.model.ConsentStatus;
import com.medisphere.consent.repository.ConsentRepository;
import com.medisphere.patient.model.Patient;
import com.medisphere.patient.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConsentServiceTest {

    @Mock
    private ConsentRepository consentRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private ConsentService consentService;

    @BeforeEach
    void setUp() {
        when(patientRepository.existsById("pat-001")).thenReturn(true);
        when(patientRepository.existsById("pat-002")).thenReturn(true);
    }

    @Test
    @DisplayName("grantConsent succeeds for PATIENT granting own consent")
    void testGrantConsentPatientSuccess() {
        User patientUser = new User("john_doe", "john@test.org", "hash", Role.PATIENT, "pat-001", null);
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(patientUser));

        User providerUser = new User("dr_smith", "dr@test.org", "hash", Role.PROVIDER, null, "prov-001");
        when(userRepository.findByLinkedProviderId("prov-001")).thenReturn(Optional.of(providerUser));

        Consent oldConsent = new Consent();
        oldConsent.setId("c-old");
        oldConsent.setStatus(ConsentStatus.GRANTED);
        when(consentRepository.findByPatientIdAndGrantedToAndStatus("pat-001", "prov-001", ConsentStatus.GRANTED))
                .thenReturn(List.of(oldConsent));

        when(consentRepository.save(any(Consent.class))).thenAnswer(inv -> {
            Consent c = inv.getArgument(0);
            if (c.getId() == null) c.setId("c-new");
            return c;
        });

        ConsentCreateRequest request = new ConsentCreateRequest();
        request.setGrantedTo("prov-001");
        request.setScope("READ_ALL");
        request.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));

        ConsentDTO result = consentService.grantConsent("pat-001", request, "john_doe");

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ConsentStatus.GRANTED);
        assertThat(result.getPatientId()).isEqualTo("pat-001");
        assertThat(result.getGrantedTo()).isEqualTo("prov-001");

        // Old consent should be superseded (REVOKED)
        assertThat(oldConsent.getStatus()).isEqualTo(ConsentStatus.REVOKED);
        verify(consentRepository).save(oldConsent);

        // Audit log verified
        verify(auditService).log(eq(AuditAction.CONSENT_GRANTED), eq("CONSENT"), eq("c-new"),
                eq("pat-001"), contains("prov-001"), eq(AuditOutcome.SUCCESS));
    }

    @Test
    @DisplayName("grantConsent fails when patientId does not match PATIENT user's linkedPatientId")
    void testGrantConsentPatientWrongPatientId() {
        User providerUser = new User("dr_smith", "dr@test.org", "hash", Role.PROVIDER, null, "prov-001");
        when(userRepository.findByLinkedProviderId("prov-001")).thenReturn(Optional.of(providerUser));

        User patientUser = new User("john_doe", "john@test.org", "hash", Role.PATIENT, "pat-001", null);
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(patientUser));

        ConsentCreateRequest request = new ConsentCreateRequest();
        request.setGrantedTo("prov-001");
        request.setScope("READ_ALL");

        assertThatThrownBy(() -> consentService.grantConsent("pat-002", request, "john_doe"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Patients may only grant consent for their own record");
    }

    @Test
    @DisplayName("grantConsent fails when called by a PROVIDER")
    void testGrantConsentProviderForbidden() {
        User providerUser = new User("dr_smith", "dr@test.org", "hash", Role.PROVIDER, null, "prov-001");
        when(userRepository.findByLinkedProviderId("prov-001")).thenReturn(Optional.of(providerUser));
        when(userRepository.findByUsername("dr_smith")).thenReturn(Optional.of(providerUser));

        ConsentCreateRequest request = new ConsentCreateRequest();
        request.setGrantedTo("prov-001");
        request.setScope("READ_ALL");

        assertThatThrownBy(() -> consentService.grantConsent("pat-001", request, "dr_smith"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Providers are not permitted to grant consent");
    }

    @Test
    @DisplayName("grantConsent fails when grantedTo provider does not exist")
    void testGrantConsentInvalidProvider() {
        when(userRepository.findByLinkedProviderId("prov-nonexistent")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("prov-nonexistent")).thenReturn(Optional.empty());

        ConsentCreateRequest request = new ConsentCreateRequest();
        request.setGrantedTo("prov-nonexistent");
        request.setScope("READ_ALL");
        request.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));

        assertThatThrownBy(() -> consentService.grantConsent("pat-001", request, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid provider identifier");
    }

    @Test
    @DisplayName("grantConsent fails when expiresAt is in the past")
    void testGrantConsentPastExpiry() {
        User providerUser = new User("dr_smith", "dr@test.org", "hash", Role.PROVIDER, null, "prov-001");
        when(userRepository.findByLinkedProviderId("prov-001")).thenReturn(Optional.of(providerUser));

        User adminUser = new User("admin", "admin@test.org", "hash", Role.ADMIN, null, null);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        ConsentCreateRequest request = new ConsentCreateRequest();
        request.setGrantedTo("prov-001");
        request.setScope("READ_ALL");
        request.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));

        assertThatThrownBy(() -> consentService.grantConsent("pat-001", request, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be in the future");
    }

    @Test
    @DisplayName("revokeConsent succeeds for PATIENT revoking own consent")
    void testRevokeConsentSuccess() {
        User patientUser = new User("john_doe", "john@test.org", "hash", Role.PATIENT, "pat-001", null);
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(patientUser));

        Consent consent = new Consent();
        consent.setId("c-1");
        consent.setPatientId("pat-001");
        consent.setGrantedTo("prov-001");
        consent.setStatus(ConsentStatus.GRANTED);
        when(consentRepository.findById("c-1")).thenReturn(Optional.of(consent));
        when(consentRepository.save(any(Consent.class))).thenAnswer(inv -> inv.getArgument(0));

        ConsentDTO revoked = consentService.revokeConsent("pat-001", "c-1", "john_doe");

        assertThat(revoked.getStatus()).isEqualTo(ConsentStatus.REVOKED);
        assertThat(revoked.getRevokedAt()).isNotNull();

        verify(auditService).log(eq(AuditAction.CONSENT_REVOKED), eq("CONSENT"), eq("c-1"),
                eq("pat-001"), contains("prov-001"), eq(AuditOutcome.SUCCESS));
    }

    @Test
    @DisplayName("revokeConsent fails for PROVIDER")
    void testRevokeConsentProviderForbidden() {
        Consent consent = new Consent();
        consent.setId("c-1");
        consent.setPatientId("pat-001");
        consent.setGrantedTo("prov-001");
        consent.setStatus(ConsentStatus.GRANTED);
        when(consentRepository.findById("c-1")).thenReturn(Optional.of(consent));

        User providerUser = new User("dr_smith", "dr@test.org", "hash", Role.PROVIDER, null, "prov-001");
        when(userRepository.findByUsername("dr_smith")).thenReturn(Optional.of(providerUser));

        assertThatThrownBy(() -> consentService.revokeConsent("pat-001", "c-1", "dr_smith"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Providers are not permitted to revoke consent");
    }

    @Test
    @DisplayName("hasActiveConsent returns true for valid GRANTED consent")
    void testHasActiveConsentTrue() {
        Consent consent = new Consent();
        consent.setId("c-1");
        consent.setPatientId("pat-001");
        consent.setGrantedTo("prov-001");
        consent.setStatus(ConsentStatus.GRANTED);
        consent.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));

        when(consentRepository.findFirstByPatientIdAndGrantedToAndStatusOrderByCreatedAtDesc("pat-001", "prov-001", ConsentStatus.GRANTED))
                .thenReturn(Optional.of(consent));

        boolean active = consentService.hasActiveConsent("pat-001", "prov-001");
        assertThat(active).isTrue();
    }

    @Test
    @DisplayName("hasActiveConsent returns false for expired consent")
    void testHasActiveConsentExpired() {
        Consent consent = new Consent();
        consent.setId("c-1");
        consent.setPatientId("pat-001");
        consent.setGrantedTo("prov-001");
        consent.setStatus(ConsentStatus.GRANTED);
        consent.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS)); // Expired

        when(consentRepository.findFirstByPatientIdAndGrantedToAndStatusOrderByCreatedAtDesc("pat-001", "prov-001", ConsentStatus.GRANTED))
                .thenReturn(Optional.of(consent));

        boolean active = consentService.hasActiveConsent("pat-001", "prov-001");
        assertThat(active).isFalse();
    }

    @Test
    @DisplayName("verifyConsent returns detailed response and audits CONSENT_VERIFIED")
    void testVerifyConsent() {
        Consent consent = new Consent();
        consent.setId("c-1");
        consent.setPatientId("pat-001");
        consent.setGrantedTo("prov-001");
        consent.setScope("READ_ALL");
        consent.setStatus(ConsentStatus.GRANTED);
        consent.setExpiresAt(Instant.now().plus(10, ChronoUnit.DAYS));

        when(consentRepository.findFirstByPatientIdAndGrantedToAndStatusOrderByCreatedAtDesc("pat-001", "prov-001", ConsentStatus.GRANTED))
                .thenReturn(Optional.of(consent));

        ConsentVerifyResponse response = consentService.verifyConsent("pat-001", "prov-001");

        assertThat(response.isHasConsent()).isTrue();
        assertThat(response.getConsentId()).isEqualTo("c-1");
        assertThat(response.getScope()).isEqualTo("READ_ALL");

        verify(auditService).log(eq(AuditAction.CONSENT_VERIFIED), eq("CONSENT"), eq("c-1"),
                eq("pat-001"), contains("Active consent verified for provider prov-001"), eq(AuditOutcome.SUCCESS));
    }
}
