package com.medisphere.risk.controller;

import com.medisphere.audit.annotation.Audited;
import com.medisphere.audit.model.AuditAction;
import com.medisphere.risk.dto.EncounterContextDTO;
import com.medisphere.risk.dto.RiskPredictionResponseDTO;
import com.medisphere.risk.model.RiskPrediction;
import com.medisphere.risk.service.RiskAssessmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Secure REST Controller exposing clinical risk estimation and SHAP explainability endpoints.
 * Enforces patient-ownership, provider assignment, active consent, and HIPAA audit logging.
 */
@RestController
@RequestMapping("/api/patients/{patientId}/risk")
public class RiskAssessmentController {

    private final RiskAssessmentService riskAssessmentService;

    public RiskAssessmentController(RiskAssessmentService riskAssessmentService) {
        this.riskAssessmentService = riskAssessmentService;
    }

    /**
     * Evaluates 10-year Cardiovascular Disease risk and computes local SHAP feature attributions
     * from the patient's existing HealthTwin.
     */
    @PostMapping("/cardiovascular")
    @PreAuthorize("@sec.canAccessPatientWithConsent(#patientId)")
    @Audited(action = AuditAction.ML_PREDICTION, resourceType = "RISK_PREDICTION")
    public ResponseEntity<RiskPredictionResponseDTO> evaluateCardiovascular(
            @PathVariable String patientId,
            @RequestParam(required = false) String benchmarkModel) {

        String authorizedModel = sanitizeBenchmarkModel(benchmarkModel);
        RiskPredictionResponseDTO response = riskAssessmentService.evaluateCardiovascularRisk(patientId, authorizedModel);
        return ResponseEntity.ok(response);
    }

    /**
     * Evaluates Diabetes Inpatient Complications risk and computes local SHAP feature attributions.
     * Combines patient demographics with an explicit EncounterContextDTO.
     */
    @PostMapping("/diabetes")
    @PreAuthorize("@sec.canAccessPatientWithConsent(#patientId)")
    @Audited(action = AuditAction.ML_PREDICTION, resourceType = "RISK_PREDICTION")
    public ResponseEntity<RiskPredictionResponseDTO> evaluateDiabetes(
            @PathVariable String patientId,
            @Valid @RequestBody EncounterContextDTO context,
            @RequestParam(required = false) String benchmarkModel) {

        String authorizedModel = sanitizeBenchmarkModel(benchmarkModel);
        RiskPredictionResponseDTO response = riskAssessmentService.evaluateDiabetesRisk(patientId, context, authorizedModel);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the chronological risk evaluation history and historical SHAP attributions for a patient.
     */
    @GetMapping("/history")
    @PreAuthorize("@sec.canAccessPatientWithConsent(#patientId)")
    public ResponseEntity<List<RiskPrediction>> getRiskHistory(@PathVariable String patientId) {
        List<RiskPrediction> history = riskAssessmentService.getRiskHistory(patientId);
        return ResponseEntity.ok(history);
    }

    /**
     * Ensures only administrators can request non-default research benchmark models.
     * Regular providers and patients strictly execute against production federated consensus.
     */
    private String sanitizeBenchmarkModel(String requestedModel) {
        if (requestedModel == null || requestedModel.isBlank()) {
            return null;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
            return requestedModel;
        }
        return null; // Ignore benchmark request if caller is not an administrator
    }
}
