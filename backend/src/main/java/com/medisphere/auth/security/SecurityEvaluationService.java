package com.medisphere.auth.security;

import com.medisphere.audit.model.AuditAction;
import com.medisphere.audit.model.AuditOutcome;
import com.medisphere.audit.service.AuditService;
import com.medisphere.auth.model.Role;
import com.medisphere.auth.model.User;
import com.medisphere.auth.repository.UserRepository;
import com.medisphere.consent.service.ConsentService;
import com.medisphere.patient.model.Patient;
import com.medisphere.patient.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Custom Spring Security evaluation bean for fine-grained, role-based patient access control.
 *
 * <p>Enforces:
 * <ul>
 *   <li>{@code ADMIN}: Unrestricted access to all patients and twins.</li>
 *   <li>{@code PROVIDER}: Access restricted strictly to assigned patients and active consent grants.</li>
 *   <li>{@code PATIENT}: Access restricted strictly to their own patient ID (via {@code linkedPatientId}).</li>
 * </ul>
 * </p>
 */
@Component("sec")
public class SecurityEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(SecurityEvaluationService.class);

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final ConsentService consentService;
    private final AuditService auditService;

    public SecurityEvaluationService(UserRepository userRepository,
                                   PatientRepository patientRepository,
                                   ConsentService consentService,
                                   AuditService auditService) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.consentService = consentService;
        this.auditService = auditService;
    }

    /**
     * Determines whether the currently authenticated principal is authorized to access
     * the specified patient's record or digital twin based on role and assignment (without requiring consent).
     */
    public boolean canAccessPatient(String patientId) {
        if (!StringUtils.hasText(patientId)) {
            return false;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null || !user.isActive()) {
            return false;
        }

        // 1. ADMIN has global access
        if (user.getRole() == Role.ADMIN) {
            return true;
        }

        // 2. PATIENT can only access their own linked patient record
        if (user.getRole() == Role.PATIENT) {
            boolean isOwnRecord = patientId.equals(user.getLinkedPatientId());
            if (!isOwnRecord) {
                log.warn("Access denied: Patient '{}' attempted to access unauthorized patient '{}'",
                        user.getUsername(), patientId);
                auditService.log(
                        user.getId(), user.getUsername(), user.getRole().name(),
                        AuditAction.ACCESS_DENIED, "PATIENT", patientId, patientId,
                        "Access denied: patient attempted to access unauthorized patient record",
                        AuditOutcome.DENIED, null
                );
            }
            return isOwnRecord;
        }

        // 3. PROVIDER can only access patients assigned to their linkedProviderId
        if (user.getRole() == Role.PROVIDER) {
            String providerId = user.getLinkedProviderId();
            if (!StringUtils.hasText(providerId)) {
                log.warn("Access denied: Provider '{}' has no linkedProviderId configured", user.getUsername());
                auditService.log(
                        user.getId(), user.getUsername(), user.getRole().name(),
                        AuditAction.ACCESS_DENIED, "PATIENT", patientId, patientId,
                        "Access denied: provider has no linkedProviderId configured",
                        AuditOutcome.DENIED, null
                );
                return false;
            }

            Patient patient = patientRepository.findById(patientId).orElse(null);
            if (patient == null) {
                // Return true so controller/service can return clean 404 Not Found
                return true;
            }

            boolean isAssigned = patient.getAssignedProviderIds() != null
                    && patient.getAssignedProviderIds().contains(providerId);
            if (!isAssigned) {
                log.warn("Access denied: Provider '{}' (ID: {}) is not assigned to patient '{}'",
                        user.getUsername(), providerId, patientId);
                auditService.log(
                        user.getId(), user.getUsername(), user.getRole().name(),
                        AuditAction.ACCESS_DENIED, "PATIENT", patientId, patientId,
                        "Access denied: provider " + providerId + " is not assigned to patient",
                        AuditOutcome.DENIED, null
                );
            }
            return isAssigned;
        }

        return false;
    }

    /**
     * Determines whether the currently authenticated principal is authorized to access
     * the specified patient's protected clinical record (twin, vitals, labs), which additionally
     * requires an active consent grant for healthcare providers.
     */
    public boolean canAccessPatientWithConsent(String patientId) {
        if (!canAccessPatient(patientId)) {
            return false;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null || !user.isActive()) {
            return false;
        }

        // ADMIN and PATIENT (own record) do not require third-party consent
        if (user.getRole() == Role.ADMIN || user.getRole() == Role.PATIENT) {
            return true;
        }

        // PROVIDER requires active, non-expired consent
        if (user.getRole() == Role.PROVIDER) {
            String providerId = user.getLinkedProviderId();
            boolean hasActiveConsent = consentService.hasActiveConsent(patientId, providerId);
            if (!hasActiveConsent) {
                log.warn("Access denied: Provider '{}' (ID: {}) lacks active consent for patient '{}'",
                        user.getUsername(), providerId, patientId);
                auditService.log(
                        user.getId(), user.getUsername(), user.getRole().name(),
                        AuditAction.ACCESS_DENIED, "PATIENT", patientId, patientId,
                        "Access denied: no active consent found for provider " + providerId,
                        AuditOutcome.DENIED, null
                );
                return false;
            }
            return true;
        }

        return false;
    }

    /**
     * Determines whether the currently authenticated principal is authorized to access
     * a specific FHIR resource document.
     */
    public boolean canAccessFhirResource(com.medisphere.fhir.model.FhirResource resource) {
        if (resource == null) {
            return false;
        }
        if (!StringUtils.hasText(resource.getPatientId())) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            return auth != null && auth.getAuthorities().stream().anyMatch(a ->
                    "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_PROVIDER".equals(a.getAuthority()));
        }
        return canAccessPatientWithConsent(resource.getPatientId());
    }
}

