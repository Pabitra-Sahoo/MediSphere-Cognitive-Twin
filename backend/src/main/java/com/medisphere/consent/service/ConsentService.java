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
import com.medisphere.patient.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Core business service for managing patient consents, lifecycle transitions, and active verification.
 */
@Service
public class ConsentService {

    private static final Logger log = LoggerFactory.getLogger(ConsentService.class);

    private final ConsentRepository consentRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public ConsentService(ConsentRepository consentRepository,
                          PatientRepository patientRepository,
                          UserRepository userRepository,
                          AuditService auditService) {
        this.consentRepository = consentRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    /**
     * Deterministically checks whether an active, non-expired consent grant exists for the given patient and provider.
     */
    public boolean hasActiveConsent(String patientId, String providerId) {
        if (!StringUtils.hasText(patientId) || !StringUtils.hasText(providerId)) {
            return false;
        }

        Optional<Consent> consentOpt = consentRepository
                .findFirstByPatientIdAndGrantedToAndStatusOrderByCreatedAtDesc(patientId, providerId, ConsentStatus.GRANTED);

        if (consentOpt.isEmpty()) {
            return false;
        }

        Consent consent = consentOpt.get();
        boolean notExpired = (consent.getExpiresAt() == null || consent.getExpiresAt().isAfter(Instant.now()));
        return notExpired;
    }

    /**
     * Retrieves all consent records for a patient.
     */
    public List<ConsentDTO> getConsents(String patientId) {
        validatePatientExists(patientId);
        List<Consent> consents = consentRepository.findByPatientId(patientId);
        return consents.stream()
                .map(c -> new ConsentDTO(c, resolveProviderName(c.getGrantedTo())))
                .toList();
    }

    /**
     * Grants a new consent for a provider.
     * Enforces validation rules:
     * <ul>
     *   <li>Patient must exist.</li>
     *   <li>grantedTo must resolve to an existing provider.</li>
     *   <li>Patient may grant only for their own record.</li>
     *   <li>Provider cannot grant consent to themselves or others.</li>
     *   <li>expiresAt, when supplied, must be in the future.</li>
     *   <li>Supersedes any existing active consent for the (patientId, grantedTo) pair.</li>
     * </ul>
     */
    public ConsentDTO grantConsent(String patientId, ConsentCreateRequest request, String actorUsername) {
        validatePatientExists(patientId);

        if (!StringUtils.hasText(request.getGrantedTo())) {
            throw new IllegalArgumentException("grantedTo provider identifier is required");
        }
        if (!StringUtils.hasText(request.getScope())) {
            throw new IllegalArgumentException("scope is required");
        }

        // 1. Validate grantedTo resolves to an existing PROVIDER
        User providerUser = resolveProviderUser(request.getGrantedTo());
        if (providerUser == null || providerUser.getRole() != Role.PROVIDER) {
            throw new IllegalArgumentException("Invalid provider identifier: " + request.getGrantedTo());
        }

        // 2. Validate caller permissions
        User actor = userRepository.findByUsername(actorUsername).orElse(null);
        if (actor == null) {
            throw new AccessDeniedException("Unauthenticated user cannot grant consent");
        }

        if (actor.getRole() == Role.PATIENT) {
            if (!patientId.equals(actor.getLinkedPatientId())) {
                throw new AccessDeniedException("Patients may only grant consent for their own record");
            }
        } else if (actor.getRole() == Role.PROVIDER) {
            throw new AccessDeniedException("Providers are not permitted to grant consent");
        } else if (actor.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Unauthorized role for granting consent");
        }

        // 3. Validate expiration date
        if (request.getExpiresAt() != null && !request.getExpiresAt().isAfter(Instant.now())) {
            throw new IllegalArgumentException("Expiration date must be in the future");
        }

        // 4. Supersede any existing active GRANTED consent for this (patientId, grantedTo) pair
        List<Consent> existingActive = consentRepository
                .findByPatientIdAndGrantedToAndStatus(patientId, request.getGrantedTo(), ConsentStatus.GRANTED);
        for (Consent prior : existingActive) {
            prior.setStatus(ConsentStatus.REVOKED);
            prior.setRevokedAt(Instant.now());
            prior.setReason("Superseded by new consent");
            prior.setUpdatedAt(Instant.now());
            consentRepository.save(prior);
        }

        // 5. Persist new active consent
        Consent consent = new Consent(
                patientId,
                request.getGrantedTo(),
                request.getScope(),
                ConsentStatus.GRANTED,
                Instant.now(),
                request.getExpiresAt(),
                request.getReason()
        );
        Consent saved = consentRepository.save(consent);

        // 6. Record audit event
        auditService.log(
                AuditAction.CONSENT_GRANTED,
                "CONSENT",
                saved.getId(),
                patientId,
                "Consent granted to provider " + request.getGrantedTo() + " (scope: " + request.getScope() + ")",
                AuditOutcome.SUCCESS
        );

        return new ConsentDTO(saved, resolveProviderName(saved.getGrantedTo()));
    }

    /**
     * Revokes an existing consent.
     */
    public ConsentDTO revokeConsent(String patientId, String consentId, String actorUsername) {
        validatePatientExists(patientId);

        Consent consent = consentRepository.findById(consentId)
                .orElseThrow(() -> new ResourceNotFoundException("Consent not found: " + consentId));

        if (!patientId.equals(consent.getPatientId())) {
            throw new IllegalArgumentException("Consent " + consentId + " does not belong to patient " + patientId);
        }

        User actor = userRepository.findByUsername(actorUsername).orElse(null);
        if (actor == null) {
            throw new AccessDeniedException("Unauthenticated user cannot revoke consent");
        }

        if (actor.getRole() == Role.PATIENT) {
            if (!patientId.equals(actor.getLinkedPatientId())) {
                throw new AccessDeniedException("Patients may only revoke consent for their own record");
            }
        } else if (actor.getRole() == Role.PROVIDER) {
            throw new AccessDeniedException("Providers are not permitted to revoke consent");
        } else if (actor.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Unauthorized role for revoking consent");
        }

        consent.setStatus(ConsentStatus.REVOKED);
        consent.setRevokedAt(Instant.now());
        consent.setUpdatedAt(Instant.now());
        Consent saved = consentRepository.save(consent);

        auditService.log(
                AuditAction.CONSENT_REVOKED,
                "CONSENT",
                saved.getId(),
                patientId,
                "Consent revoked for provider " + saved.getGrantedTo(),
                AuditOutcome.SUCCESS
        );

        return new ConsentDTO(saved, resolveProviderName(saved.getGrantedTo()));
    }

    /**
     * Checks and audits consent verification for a provider.
     */
    public ConsentVerifyResponse verifyConsent(String patientId, String providerId) {
        validatePatientExists(patientId);

        Optional<Consent> consentOpt = consentRepository
                .findFirstByPatientIdAndGrantedToAndStatusOrderByCreatedAtDesc(patientId, providerId, ConsentStatus.GRANTED);

        boolean hasConsent = false;
        String consentId = null;
        String scope = null;
        Instant expiresAt = null;

        if (consentOpt.isPresent()) {
            Consent consent = consentOpt.get();
            if (consent.getExpiresAt() == null || consent.getExpiresAt().isAfter(Instant.now())) {
                hasConsent = true;
                consentId = consent.getId();
                scope = consent.getScope();
                expiresAt = consent.getExpiresAt();
            }
        }

        auditService.log(
                AuditAction.CONSENT_VERIFIED,
                "CONSENT",
                consentId != null ? consentId : "none",
                patientId,
                hasConsent ? "Active consent verified for provider " + providerId
                           : "No active consent found for provider " + providerId,
                hasConsent ? AuditOutcome.SUCCESS : AuditOutcome.DENIED
        );

        return new ConsentVerifyResponse(hasConsent, consentId, scope, expiresAt);
    }

    private void validatePatientExists(String patientId) {
        if (!patientRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Patient not found: " + patientId);
        }
    }

    private User resolveProviderUser(String providerIdOrUserId) {
        if (!StringUtils.hasText(providerIdOrUserId)) {
            return null;
        }
        // Match by linkedProviderId first
        User user = userRepository.findByLinkedProviderId(providerIdOrUserId).orElse(null);
        if (user != null) {
            return user;
        }
        // Match by user ID second
        return userRepository.findById(providerIdOrUserId).orElse(null);
    }

    public String resolveProviderName(String providerId) {
        User user = resolveProviderUser(providerId);
        if (user != null) {
            return user.getUsername();
        }
        return providerId;
    }
}
