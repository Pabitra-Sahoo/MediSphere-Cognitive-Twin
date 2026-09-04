package com.medisphere.fhir.controller;

import com.medisphere.auth.security.SecurityEvaluationService;
import com.medisphere.common.exception.ResourceNotFoundException;
import com.medisphere.fhir.dto.FhirIngestionResult;
import com.medisphere.fhir.dto.FhirResourceSummaryDTO;
import com.medisphere.fhir.model.FhirResource;
import com.medisphere.fhir.repository.FhirResourceRepository;
import com.medisphere.fhir.service.FhirIngestionService;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller for FHIR R4 ingestion and resource retrieval.
 */
@RestController
public class FhirController {

    private final FhirIngestionService fhirIngestionService;
    private final FhirResourceRepository fhirResourceRepository;
    private final SecurityEvaluationService securityEvaluationService;

    public FhirController(FhirIngestionService fhirIngestionService,
                          FhirResourceRepository fhirResourceRepository,
                          SecurityEvaluationService securityEvaluationService) {
        this.fhirIngestionService = fhirIngestionService;
        this.fhirResourceRepository = fhirResourceRepository;
        this.securityEvaluationService = securityEvaluationService;
    }

    /**
     * Ingests a FHIR R4 Bundle or individual resource.
     * Restricted to ADMIN and system integration.
     */
    @PostMapping(
            value = "/api/fhir/ingest",
            consumes = {MediaType.APPLICATION_JSON_VALUE, "application/fhir+json"},
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FhirIngestionResult> ingestFhir(@RequestBody String rawJson) {
        FhirIngestionResult result = fhirIngestionService.ingest(rawJson);
        return ResponseEntity.ok(result);
    }

    /**
     * Retrieves stored FHIR resources for a specific patient.
     * Enforces fine-grained patient/provider/admin RBAC.
     */
    @GetMapping(value = "/api/patients/{patientId}/fhir-resources", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@sec.canAccessPatientWithConsent(#patientId)")
    public ResponseEntity<Map<String, Object>> getPatientFhirResources(
            @PathVariable String patientId,
            @RequestParam(required = false) String resourceType) {

        Sort sort = Sort.by(Sort.Direction.DESC, "processedAt");
        List<FhirResource> resources;

        if (StringUtils.hasText(resourceType)) {
            resources = fhirResourceRepository.findByPatientIdAndResourceType(patientId, resourceType, sort);
        } else {
            resources = fhirResourceRepository.findByPatientId(patientId, sort);
        }

        List<FhirResourceSummaryDTO> summaries = resources.stream()
                .map(r -> new FhirResourceSummaryDTO(
                        r.getId(),
                        r.getResourceType(),
                        r.getResourceId(),
                        r.getValidationStatus(),
                        r.getProcessedAt()
                ))
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("content", summaries);
        response.put("totalElements", summaries.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a full FHIR resource by its resourceId.
     * Restricted to ADMIN and PROVIDER (with patient assignment enforcement).
     */
    @GetMapping(value = "/api/fhir/resources/{resourceId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVIDER')")
    public ResponseEntity<FhirResource> getFhirResourceById(@PathVariable String resourceId) {
        FhirResource resource = fhirResourceRepository.findByResourceId(resourceId)
                .or(() -> fhirResourceRepository.findById(resourceId))
                .orElseThrow(() -> new ResourceNotFoundException("FHIR Resource", resourceId));

        if (!securityEvaluationService.canAccessFhirResource(resource)) {
            throw new AccessDeniedException("Access denied: You are not authorized to view this FHIR resource");
        }

        return ResponseEntity.ok(resource);
    }
}
