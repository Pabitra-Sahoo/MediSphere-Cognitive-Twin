package com.medisphere.vitals.controller;

import com.medisphere.vitals.dto.VitalsDTO;
import com.medisphere.vitals.dto.VitalsEventDTO;
import com.medisphere.vitals.dto.VitalsSimulateRequest;
import com.medisphere.vitals.service.VitalsService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

/**
 * REST controller for vital signs simulation ingestion and historical queries.
 */
@RestController
@RequestMapping("/api")
public class VitalsController {

    private final VitalsService vitalsService;

    public VitalsController(VitalsService vitalsService) {
        this.vitalsService = vitalsService;
    }

    /**
     * Publishes a simulated vitals packet to the Kafka ingestion pipeline.
     * Accessible only by ADMIN users.
     */
    @PostMapping("/vitals/simulate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> simulateVitals(@Valid @RequestBody VitalsSimulateRequest request) {
        VitalsEventDTO event = vitalsService.simulateVitals(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "status", "ACCEPTED",
                "message", "Vitals simulation event submitted to Kafka ingest pipeline",
                "eventId", event.getEventId(),
                "patientId", event.getPatientId()
        ));
    }

    /**
     * Retrieves historical vital signs measurements for a patient with pagination and date range filters.
     * Enforces fine-grained patient access control (ADMIN, assigned PROVIDER, or self PATIENT).
     */
    @GetMapping("/patients/{patientId}/vitals")
    @PreAuthorize("hasRole('ADMIN') or @sec.canAccessPatient(#patientId)")
    public ResponseEntity<Page<VitalsDTO>> getVitalsHistory(
            @PathVariable String patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "recordedAt"));
        Page<VitalsDTO> vitalsPage = vitalsService.getVitalsHistory(patientId, from, to, pageRequest);
        return ResponseEntity.ok(vitalsPage);
    }

    /**
     * Retrieves the latest vital signs reading for a patient.
     * Enforces fine-grained patient access control (ADMIN, assigned PROVIDER, or self PATIENT).
     */
    @GetMapping("/patients/{patientId}/vitals/latest")
    @PreAuthorize("hasRole('ADMIN') or @sec.canAccessPatient(#patientId)")
    public ResponseEntity<VitalsDTO> getLatestVitals(@PathVariable String patientId) {
        VitalsDTO dto = vitalsService.getLatestVitals(patientId);
        return ResponseEntity.ok(dto);
    }
}
