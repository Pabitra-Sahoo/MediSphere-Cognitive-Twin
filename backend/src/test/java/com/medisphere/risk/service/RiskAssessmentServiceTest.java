package com.medisphere.risk.service;

import com.medisphere.common.exception.ResourceNotFoundException;
import com.medisphere.risk.adapter.PatientClinicalFeatureAdapter;
import com.medisphere.risk.client.MlServiceClient;
import com.medisphere.risk.dto.EncounterContextDTO;
import com.medisphere.risk.dto.FeatureAttributionDTO;
import com.medisphere.risk.dto.RiskPredictionResponseDTO;
import com.medisphere.risk.model.RiskPrediction;
import com.medisphere.risk.repository.RiskPredictionRepository;
import com.medisphere.twin.model.HealthTwin;
import com.medisphere.twin.repository.HealthTwinRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiskAssessmentServiceTest {

    @Mock
    private HealthTwinRepository healthTwinRepository;

    @Mock
    private PatientClinicalFeatureAdapter featureAdapter;

    @Mock
    private MlServiceClient mlServiceClient;

    @Mock
    private RiskPredictionRepository riskPredictionRepository;

    private RiskAssessmentService service;

    @BeforeEach
    void setUp() {
        service = new RiskAssessmentService(
                healthTwinRepository,
                featureAdapter,
                mlServiceClient,
                riskPredictionRepository
        );
    }

    private RiskPredictionResponseDTO createSampleResponse(String taskType, String modelName) {
        return new RiskPredictionResponseDTO(
                taskType,
                modelName,
                "federated_flwr_fedavg",
                0.28,
                "LOW",
                0.5,
                "log_odds",
                -0.619,
                -0.944,
                List.of(new FeatureAttributionDTO("age", 55.0, -0.15, "DECREASES_RISK", 1)),
                "test-safety-disclaimer"
        );
    }

    @Test
    @DisplayName("evaluateCardiovascularRisk: successfully coordinates feature extraction, inference, twin update and persistence")
    void testEvaluateCardiovascularRiskSuccess() {
        HealthTwin twin = new HealthTwin();
        twin.setId("twin-001");
        twin.setPatientId("pat-001");
        twin.setRiskScores(new HashMap<>());

        when(healthTwinRepository.findByPatientId("pat-001")).thenReturn(Optional.of(twin));
        when(featureAdapter.extractCardiovascularFeatures(twin)).thenReturn(Map.of("age", 55.0));

        RiskPredictionResponseDTO mlResponse = createSampleResponse("cardiovascular_risk", "cardiovascular_federated_logistic_regression");
        when(mlServiceClient.predictAndExplain(eq("CARDIOVASCULAR"), eq("cardiovascular_federated_logistic_regression"), any()))
                .thenReturn(mlResponse);

        RiskPredictionResponseDTO result = service.evaluateCardiovascularRisk("pat-001", null);

        assertNotNull(result);
        assertEquals(0.28, result.getModelEstimatedRiskProbability());
        assertEquals("LOW", result.getEstimatedRiskTier());

        // Verify HealthTwin riskScores update
        ArgumentCaptor<HealthTwin> twinCaptor = ArgumentCaptor.forClass(HealthTwin.class);
        verify(healthTwinRepository).save(twinCaptor.capture());
        HealthTwin savedTwin = twinCaptor.getValue();
        assertNotNull(savedTwin.getRiskScores().get("cardiovascularRisk"));

        // Verify RiskPrediction persistence
        ArgumentCaptor<RiskPrediction> predictionCaptor = ArgumentCaptor.forClass(RiskPrediction.class);
        verify(riskPredictionRepository).save(predictionCaptor.capture());
        RiskPrediction savedPred = predictionCaptor.getValue();
        assertEquals("pat-001", savedPred.getPatientId());
        assertEquals("cardiovascular_risk", savedPred.getTaskType());
        assertEquals("cardiovascular_federated_logistic_regression", savedPred.getModelName());
    }

    @Test
    @DisplayName("evaluateCardiovascularRisk: uses custom benchmark model when requested")
    void testEvaluateCardiovascularRiskBenchmarkModel() {
        HealthTwin twin = new HealthTwin();
        twin.setId("twin-001");
        twin.setPatientId("pat-001");

        when(healthTwinRepository.findByPatientId("pat-001")).thenReturn(Optional.of(twin));
        when(featureAdapter.extractCardiovascularFeatures(twin)).thenReturn(Map.of("age", 55.0));

        RiskPredictionResponseDTO mlResponse = createSampleResponse("cardiovascular_risk", "cardiovascular_hist_gradient_boosting");
        when(mlServiceClient.predictAndExplain(eq("CARDIOVASCULAR"), eq("cardiovascular_hist_gradient_boosting"), any()))
                .thenReturn(mlResponse);

        RiskPredictionResponseDTO result = service.evaluateCardiovascularRisk("pat-001", "cardiovascular_hist_gradient_boosting");

        assertNotNull(result);
        assertEquals("cardiovascular_hist_gradient_boosting", result.getModelName());
    }

    @Test
    @DisplayName("evaluateCardiovascularRisk: throws ResourceNotFoundException if twin absent")
    void testEvaluateCardiovascularRiskTwinNotFound() {
        when(healthTwinRepository.findByPatientId("pat-nonexistent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.evaluateCardiovascularRisk("pat-nonexistent", null)
        );
    }

    @Test
    @DisplayName("evaluateDiabetesRisk: successfully extracts features, queries ML service, updates twin and persists")
    void testEvaluateDiabetesRiskSuccess() {
        HealthTwin twin = new HealthTwin();
        twin.setId("twin-001");
        twin.setPatientId("pat-001");

        EncounterContextDTO context = new EncounterContextDTO();
        context.setTimeInHospital(3);

        when(healthTwinRepository.findByPatientId("pat-001")).thenReturn(Optional.of(twin));
        when(featureAdapter.extractDiabetesFeatures(twin, context)).thenReturn(Map.of("timeInHospital", 3));

        RiskPredictionResponseDTO mlResponse = createSampleResponse("diabetes_complications", "diabetes_complications_federated_logistic_regression");
        when(mlServiceClient.predictAndExplain(eq("DIABETES"), eq("diabetes_complications_federated_logistic_regression"), any()))
                .thenReturn(mlResponse);

        RiskPredictionResponseDTO result = service.evaluateDiabetesRisk("pat-001", context, null);

        assertNotNull(result);
        assertEquals("diabetes_complications", result.getTaskType());

        // Verify Twin update
        verify(healthTwinRepository).save(any(HealthTwin.class));
        // Verify Prediction saved
        verify(riskPredictionRepository).save(any(RiskPrediction.class));
    }

    @Test
    @DisplayName("getRiskHistory: delegates to riskPredictionRepository ordered by timestamp")
    void testGetRiskHistory() {
        RiskPrediction pred = new RiskPrediction();
        pred.setId("pred-1");
        pred.setPatientId("pat-001");

        when(riskPredictionRepository.findByPatientIdOrderByEvaluatedAtDesc("pat-001"))
                .thenReturn(List.of(pred));

        List<RiskPrediction> history = service.getRiskHistory("pat-001");
        assertEquals(1, history.size());
        assertEquals("pred-1", history.get(0).getId());
    }
}
