package com.medisphere.audit.controller;

import com.medisphere.audit.dto.AuditLogDTO;
import com.medisphere.audit.model.AuditAction;
import com.medisphere.audit.service.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * REST controller for querying HIPAA-inspired audit logs.
 *
 * <p>This controller provides append-only query endpoints.
 * No endpoints exist for updating or deleting audit logs.</p>
 */
@RestController
@RequestMapping("/api")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Retrieves audit logs for a specific patient.
     * Authorized for ADMIN or the PATIENT who owns this record.
     */
    @GetMapping("/patients/{patientId}/audit-logs")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('PATIENT') and @sec.canAccessPatient(#patientId))")
    public ResponseEntity<Page<AuditLogDTO>> getPatientAuditLogs(
            @PathVariable String patientId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditLogDTO> auditLogs = auditService.getPatientAuditLogs(patientId, action, from, to, pageable);
        return ResponseEntity.ok(auditLogs);
    }

    /**
     * Retrieves system-wide audit logs with optional filters.
     * Strictly restricted to ADMIN.
     */
    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AuditLogDTO>> getAllAuditLogs(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditLogDTO> auditLogs = auditService.getAllAuditLogs(userId, action, from, to, pageable);
        return ResponseEntity.ok(auditLogs);
    }
}
