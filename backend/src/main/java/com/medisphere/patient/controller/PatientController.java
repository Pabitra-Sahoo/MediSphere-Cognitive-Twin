package com.medisphere.patient.controller;

import com.medisphere.common.dto.PagedResponse;
import com.medisphere.patient.dto.PatientDTO;
import com.medisphere.patient.dto.PatientSummaryDTO;
import com.medisphere.patient.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller exposing patient management and retrieval endpoints.
 */
@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    /**
     * Paginated list of patients.
     * Providers see only assigned patients; Admins see all.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    public ResponseEntity<PagedResponse<PatientSummaryDTO>> getPatients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            Authentication authentication) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("lastName").ascending());
        PagedResponse<PatientSummaryDTO> response = patientService.getPatients(
                authentication.getName(), search, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves full patient demographics and profile by ID.
     * Restricted to assigned providers, the patient themselves, and admins.
     */
    @GetMapping("/{patientId}")
    @PreAuthorize("@sec.canAccessPatient(#patientId)")
    public ResponseEntity<PatientDTO> getPatientById(@PathVariable String patientId) {
        PatientDTO patient = patientService.getPatientById(patientId);
        return ResponseEntity.ok(patient);
    }

    /**
     * Registers a new patient and automatically provisions their Digital Health Twin.
     * Restricted to users with the ADMIN role.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PatientDTO> createPatient(@Valid @RequestBody PatientDTO dto) {
        PatientDTO created = patientService.createPatient(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Updates an existing patient record.
     * Restricted to users with the ADMIN role.
     */
    @PutMapping("/{patientId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PatientDTO> updatePatient(
            @PathVariable String patientId,
            @Valid @RequestBody PatientDTO dto) {
        PatientDTO updated = patientService.updatePatient(patientId, dto);
        return ResponseEntity.ok(updated);
    }
}
