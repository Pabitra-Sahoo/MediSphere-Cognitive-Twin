package com.medisphere.risk.service;

import com.medisphere.common.exception.ResourceNotFoundException;
import com.medisphere.risk.adapter.PatientClinicalFeatureAdapter;
import com.medisphere.risk.client.MlServiceClient;
import com.medisphere.risk.dto.EncounterContextDTO;
import com.medisphere.risk.dto.RiskPredictionResponseDTO;
import com.medisphere.risk.model.RiskPrediction;
import com.medisphere.risk.repository.RiskPredictionRepository;
import com.medisphere.twin.model.HealthTwin;
import com.medisphere.twin.repository.HealthTwinRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service orchestrating clinical feature extraction, ML inference execution,
 * digital health twin synchronization, and regulatory audit persistence.
 */
@Service
public class RiskAssessmentService {

    private static final Logger log = LoggerFactory.getLogger(RiskAssessmentService.class);

    public static final String DEFAULT_CARDIOVASCULAR_MODEL = "cardiovascular_federated_logistic_regression";
    public static final String DEFAULT_DIABETES_MODEL = "diabetes_complications_federated_logistic_regression";

    private final HealthTwinRepository healthTwinRepository;
    private final PatientClinicalFeatureAdapter featureAdapter;
    private final MlServiceClient mlServiceClient;
    private final RiskPredictionRepository riskPredictionRepository;

    public RiskAssessmentService(
            HealthTwinRepository healthTwinRepository,
            PatientClinicalFeatureAdapter featureAdapter,
            MlServiceClient mlServiceClient,
            RiskPredictionRepository riskPredictionRepository) {
        this.healthTwinRepository = healthTwinRepository;
        this.featureAdapter = featureAdapter;
        this.mlServiceClient = mlServiceClient;
        this.riskPredictionRepository = riskPredictionRepository;
    }

    /**
     * Evaluates 10-year Cardiovascular Disease risk using the patient's existing HealthTwin measurements.
     */
    public RiskPredictionResponseDTO evaluateCardiovascularRisk(String patientId, String benchmarkModel) {
        HealthTwin twin = healthTwinRepository.findByPatientId(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("HealthTwin not found for patient: " + patientId));

        Map<String, Object> features = featureAdapter.extractCardiovascularFeatures(twin);
        String modelToUse = (benchmarkModel != null && !benchmarkModel.isBlank()) ? benchmarkModel : DEFAULT_CARDIOVASCULAR_MODEL;

        RiskPredictionResponseDTO response = mlServiceClient.predictAndExplain("CARDIOVASCULAR", modelToUse, features);

        // Update latest risk summary in HealthTwin document
        if (twin.getRiskScores() == null) {
            twin.setRiskScores(new HashMap<>());
        }
        Map<String, Object> cardioSummary = new HashMap<>();
        cardioSummary.put("modelEstimatedRiskProbability", response.getModelEstimatedRiskProbability());
        cardioSummary.put("estimatedRiskTier", response.getEstimatedRiskTier());
        cardioSummary.put("modelName", response.getModelName());
        cardioSummary.put("evaluatedAt", Instant.now().toString());
        twin.getRiskScores().put("cardiovascularRisk", cardioSummary);
        twin.setUpdatedAt(Instant.now());
        healthTwinRepository.save(twin);

        // Persist historical evaluation and its SHAP attributions for clinical accountability
        RiskPrediction predictionEntity = new RiskPrediction(
                patientId,
                response.getTaskType(),
                response.getModelName(),
                response.getModelVersion(),
                response.getModelEstimatedRiskProbability(),
                response.getEstimatedRiskTier(),
                response.getDecisionThreshold(),
                response.getExplanationSpace(),
                response.getBaseValueLogOdds(),
                response.getTotalLogOdds(),
                response.getFeatureAttributions(),
                response.getSafetyDisclaimer()
        );
        riskPredictionRepository.save(predictionEntity);

        log.info("Evaluated cardiovascular risk for patient '{}': prob={}, tier={}",
                patientId, response.getModelEstimatedRiskProbability(), response.getEstimatedRiskTier());

        return response;
    }

    /**
     * Evaluates Diabetes Inpatient Complications risk using demographics from the HealthTwin
     * and acute inpatient measurements from the EncounterContextDTO.
     */
    public RiskPredictionResponseDTO evaluateDiabetesRisk(String patientId, EncounterContextDTO context, String benchmarkModel) {
        HealthTwin twin = healthTwinRepository.findByPatientId(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("HealthTwin not found for patient: " + patientId));

        Map<String, Object> features = featureAdapter.extractDiabetesFeatures(twin, context);
        String modelToUse = (benchmarkModel != null && !benchmarkModel.isBlank()) ? benchmarkModel : DEFAULT_DIABETES_MODEL;

        RiskPredictionResponseDTO response = mlServiceClient.predictAndExplain("DIABETES", modelToUse, features);

        // Update latest risk summary in HealthTwin document
        if (twin.getRiskScores() == null) {
            twin.setRiskScores(new HashMap<>());
        }
        Map<String, Object> diabetesSummary = new HashMap<>();
        diabetesSummary.put("modelEstimatedRiskProbability", response.getModelEstimatedRiskProbability());
        diabetesSummary.put("estimatedRiskTier", response.getEstimatedRiskTier());
        diabetesSummary.put("modelName", response.getModelName());
        diabetesSummary.put("evaluatedAt", Instant.now().toString());
        twin.getRiskScores().put("diabetesComplicationsRisk", diabetesSummary);
        twin.setUpdatedAt(Instant.now());
        healthTwinRepository.save(twin);

        // Persist historical evaluation record
        RiskPrediction predictionEntity = new RiskPrediction(
                patientId,
                response.getTaskType(),
                response.getModelName(),
                response.getModelVersion(),
                response.getModelEstimatedRiskProbability(),
                response.getEstimatedRiskTier(),
                response.getDecisionThreshold(),
                response.getExplanationSpace(),
                response.getBaseValueLogOdds(),
                response.getTotalLogOdds(),
                response.getFeatureAttributions(),
                response.getSafetyDisclaimer()
        );
        riskPredictionRepository.save(predictionEntity);

        log.info("Evaluated diabetes complications risk for patient '{}': prob={}, tier={}",
                patientId, response.getModelEstimatedRiskProbability(), response.getEstimatedRiskTier());

        return response;
    }

    /**
     * Retrieves chronological risk prediction history for a patient.
     */
    public List<RiskPrediction> getRiskHistory(String patientId) {
        return riskPredictionRepository.findByPatientIdOrderByEvaluatedAtDesc(patientId);
    }
}
