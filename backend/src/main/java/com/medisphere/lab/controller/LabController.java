package com.medisphere.lab.controller;

import com.medisphere.audit.annotation.Audited;
import com.medisphere.audit.model.AuditAction;
import com.medisphere.lab.model.LabResult;
import com.medisphere.lab.repository.LabResultRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for laboratory results retrieval and entry.
 */
@RestController
@RequestMapping("/api/patients/{patientId}/labs")
public class LabController {

    private final LabResultRepository labResultRepository;

    public LabController(LabResultRepository labResultRepository) {
        this.labResultRepository = labResultRepository;
    }

    /**
     * Retrieves historical laboratory results for a patient.
     * Enforces fine-grained patient access control (ADMIN, assigned PROVIDER with active consent, or self PATIENT).
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or @sec.canAccessPatientWithConsent(#patientId)")
    @Audited(action = AuditAction.VIEW_LABS, resourceType = "LABS")
    public ResponseEntity<Page<LabResult>> getLabs(
            @PathVariable String patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "performedAt"));
        Page<LabResult> labPage = labResultRepository.findByPatientId(patientId, pageable);
        return ResponseEntity.ok(labPage);
    }

    /**
     * Creates a new laboratory result for a patient.
     * Restricted to PROVIDER or ADMIN.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('PROVIDER')")
    public ResponseEntity<LabResult> createLab(
            @PathVariable String patientId,
            @RequestBody LabResult labResult) {

        labResult.setPatientId(patientId);
        LabResult saved = labResultRepository.save(labResult);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
