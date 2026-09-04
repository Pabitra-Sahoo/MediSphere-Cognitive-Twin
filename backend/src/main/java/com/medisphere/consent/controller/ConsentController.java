package com.medisphere.consent.controller;

import com.medisphere.auth.model.User;
import com.medisphere.auth.repository.UserRepository;
import com.medisphere.consent.dto.ConsentCreateRequest;
import com.medisphere.consent.dto.ConsentDTO;
import com.medisphere.consent.dto.ConsentVerifyResponse;
import com.medisphere.consent.service.ConsentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for patient consent management and verification.
 */
@RestController
@RequestMapping("/api/patients/{patientId}/consents")
public class ConsentController {

    private final ConsentService consentService;
    private final UserRepository userRepository;

    public ConsentController(ConsentService consentService, UserRepository userRepository) {
        this.consentService = consentService;
        this.userRepository = userRepository;
    }

    /**
     * Lists all consents associated with the patient.
     * Authorized for ADMIN, the PATIENT who owns the record, or an assigned PROVIDER.
     * Note: Accessing this list does not require having an active consent.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or @sec.canAccessPatient(#patientId)")
    public ResponseEntity<List<ConsentDTO>> getConsents(@PathVariable String patientId) {
        List<ConsentDTO> consents = consentService.getConsents(patientId);
        return ResponseEntity.ok(consents);
    }

    /**
     * Grants a new consent for a healthcare provider.
     * Authorized for PATIENT (own record) or ADMIN.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or (hasRole('PATIENT') and @sec.canAccessPatient(#patientId))")
    public ResponseEntity<ConsentDTO> grantConsent(
            @PathVariable String patientId,
            @Valid @RequestBody ConsentCreateRequest request,
            Authentication authentication) {
        String actorUsername = authentication != null ? authentication.getName() : "ANONYMOUS";
        ConsentDTO created = consentService.grantConsent(patientId, request, actorUsername);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Revokes an existing consent.
     * Authorized for PATIENT (own record) or ADMIN.
     */
    @PutMapping("/{consentId}/revoke")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('PATIENT') and @sec.canAccessPatient(#patientId))")
    public ResponseEntity<ConsentDTO> revokeConsent(
            @PathVariable String patientId,
            @PathVariable String consentId,
            Authentication authentication) {
        String actorUsername = authentication != null ? authentication.getName() : "ANONYMOUS";
        ConsentDTO revoked = consentService.revokeConsent(patientId, consentId, actorUsername);
        return ResponseEntity.ok(revoked);
    }

    /**
     * Verifies whether the specified or calling provider has active consent for this patient.
     * Authorized for PROVIDER or ADMIN.
     */
    @GetMapping("/verify")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PROVIDER')")
    public ResponseEntity<ConsentVerifyResponse> verifyConsent(
            @PathVariable String patientId,
            @RequestParam(required = false) String providerId,
            Authentication authentication) {
        String targetProviderId = providerId;
        if (!StringUtils.hasText(targetProviderId) && authentication != null) {
            User user = userRepository.findByUsername(authentication.getName()).orElse(null);
            if (user != null && StringUtils.hasText(user.getLinkedProviderId())) {
                targetProviderId = user.getLinkedProviderId();
            }
        }

        if (!StringUtils.hasText(targetProviderId)) {
            return ResponseEntity.badRequest().body(ConsentVerifyResponse.noConsent());
        }

        ConsentVerifyResponse response = consentService.verifyConsent(patientId, targetProviderId);
        return ResponseEntity.ok(response);
    }
}
