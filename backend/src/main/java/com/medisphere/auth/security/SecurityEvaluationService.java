package com.medisphere.auth.security;

import com.medisphere.auth.model.Role;
import com.medisphere.auth.model.User;
import com.medisphere.auth.repository.UserRepository;
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
 *   <li>{@code PROVIDER}: Access restricted strictly to assigned patients (via {@code assignedProviderIds}).</li>
 *   <li>{@code PATIENT}: Access restricted strictly to their own patient ID (via {@code linkedPatientId}).</li>
 * </ul>
 * </p>
 */
@Component("sec")
public class SecurityEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(SecurityEvaluationService.class);

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;

    public SecurityEvaluationService(UserRepository userRepository, PatientRepository patientRepository) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
    }

    /**
     * Determines whether the currently authenticated principal is authorized to access
     * the specified patient's record or digital twin.
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
            }
            return isOwnRecord;
        }

        // 3. PROVIDER can only access patients assigned to their linkedProviderId
        if (user.getRole() == Role.PROVIDER) {
            String providerId = user.getLinkedProviderId();
            if (!StringUtils.hasText(providerId)) {
                log.warn("Access denied: Provider '{}' has no linkedProviderId configured", user.getUsername());
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
            }
            return isAssigned;
        }

        return false;
    }
}
