package com.medisphere.risk.controller;

import com.medisphere.risk.client.MlServiceClient;
import com.medisphere.risk.dto.ModelCatalogDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller providing authenticated access to the clinical model catalog
 * and validation-cohort global SHAP feature importance benchmarks.
 */
@RestController
@RequestMapping("/api/ml/models")
public class MlModelCatalogController {

    private final MlServiceClient mlServiceClient;

    public MlModelCatalogController(MlServiceClient mlServiceClient) {
        this.mlServiceClient = mlServiceClient;
    }

    /**
     * Lists registered clinical models, target tasks, and baseline metrics.
     * Restricted to authenticated clinical providers and system administrators.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVIDER')")
    public ResponseEntity<List<ModelCatalogDTO>> listModels() {
        List<ModelCatalogDTO> catalog = mlServiceClient.getModelCatalog();
        return ResponseEntity.ok(catalog);
    }

    /**
     * Retrieves the precomputed validation-cohort population SHAP importance artifact for a model.
     * Restricted to authenticated clinical providers and system administrators.
     */
    @GetMapping("/{modelName}/global-explanation")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVIDER')")
    public ResponseEntity<Map<String, Object>> getGlobalExplanation(@PathVariable String modelName) {
        Map<String, Object> globalExplanation = mlServiceClient.getGlobalExplanation(modelName);
        return ResponseEntity.ok(globalExplanation);
    }
}
