package com.medisphere.twin.controller;

import com.medisphere.audit.annotation.Audited;
import com.medisphere.audit.model.AuditAction;
import com.medisphere.twin.dto.HealthTwinDTO;
import com.medisphere.twin.dto.TwinCompletenessDTO;
import com.medisphere.twin.service.HealthTwinService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller exposing Digital Health Twin retrieval and completeness endpoints.
 */
@RestController
@RequestMapping("/api/patients/{patientId}/twin")
public class HealthTwinController {

    private final HealthTwinService healthTwinService;

    public HealthTwinController(HealthTwinService healthTwinService) {
        this.healthTwinService = healthTwinService;
    }

    /**
     * Retrieves the complete Digital Health Twin for a patient.
     * Restricted to assigned providers (with active consent), the patient themselves, and admins.
     */
    @GetMapping
    @PreAuthorize("@sec.canAccessPatientWithConsent(#patientId)")
    @Audited(action = AuditAction.VIEW_TWIN, resourceType = "TWIN")
    public ResponseEntity<HealthTwinDTO> getTwin(@PathVariable String patientId) {
        HealthTwinDTO twin = healthTwinService.getTwinByPatientId(patientId);
        return ResponseEntity.ok(twin);
    }

    /**
     * Retrieves the twin completeness calculation (evaluated against the 20 logical fields).
     * Restricted to assigned providers, the patient themselves, and admins.
     */
    @GetMapping("/completeness")
    @PreAuthorize("@sec.canAccessPatient(#patientId)")
    public ResponseEntity<TwinCompletenessDTO> getCompleteness(@PathVariable String patientId) {
        TwinCompletenessDTO completeness = healthTwinService.getCompletenessByPatientId(patientId);
        return ResponseEntity.ok(completeness);
    }
}
