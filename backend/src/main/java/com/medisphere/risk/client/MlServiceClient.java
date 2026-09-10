package com.medisphere.risk.client;

import com.medisphere.risk.dto.ModelCatalogDTO;
import com.medisphere.risk.dto.RiskPredictionResponseDTO;
import com.medisphere.risk.exception.InsufficientClinicalDataException;
import com.medisphere.risk.exception.MlServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Secure HTTP client communicating with the internal Python ML service over the private network.
 * Transmits ONLY de-identified numerical/categorical feature vectors with NO patient PII.
 */
@Component
public class MlServiceClient {

    private static final Logger log = LoggerFactory.getLogger(MlServiceClient.class);
    private static final String INTERNAL_KEY_HEADER = "X-Internal-Service-Key";

    private final RestClient restClient;
    private final String internalKey;

    public MlServiceClient(
            @Value("${ml-service.base-url:http://localhost:8000}") String baseUrl,
            @Value("${ml-service.timeout-ms:5000}") int timeoutMs,
            @Value("${ml-service.internal-key:medisphere-dev-internal-key-change-in-prod}") String internalKey) {

        this.internalKey = internalKey;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(INTERNAL_KEY_HEADER, internalKey)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();

        log.info("Initialized MlServiceClient targeting '{}' with timeout {}ms", baseUrl, timeoutMs);
    }

    /**
     * Submits a de-identified clinical feature vector to compute model-estimated risk probability
     * and local additive log-odds SHAP attributions.
     *
     * @param taskType "CARDIOVASCULAR" or "DIABETES"
     * @param modelName Optional model name override; if null, the ML service uses the production federated consensus
     * @param features De-identified feature map matching the target model's input contract
     * @return Unified prediction and explanation response DTO
     */
    public RiskPredictionResponseDTO predictAndExplain(String taskType, String modelName, Map<String, Object> features) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("task_type", taskType);
        if (modelName != null && !modelName.isBlank()) {
            payload.put("model_name", modelName);
        }
        payload.put("features", features);

        try {
            return restClient.post()
                    .uri("/api/ml/predict-and-explain")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(RiskPredictionResponseDTO.class);
        } catch (HttpClientErrorException.UnprocessableEntity ex) {
            log.warn("ML service rejected feature vector with 422 Unprocessable Entity: {}", ex.getMessage());
            throw new InsufficientClinicalDataException("Incomplete clinical feature payload received by ML service", Collections.emptyList());
        } catch (HttpClientErrorException ex) {
            log.error("ML service client error (HTTP {}): {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new MlServiceUnavailableException("ML service rejected request: " + ex.getStatusCode().value(), ex);
        } catch (ResourceAccessException ex) {
            log.error("ML service timeout or connection refused: {}", ex.getMessage());
            throw new MlServiceUnavailableException("Clinical risk estimation service is currently unreachable or timed out.", ex);
        } catch (RestClientResponseException ex) {
            log.error("ML service returned HTTP {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new MlServiceUnavailableException("Clinical risk estimation service failed with status: " + ex.getStatusCode().value(), ex);
        } catch (Exception ex) {
            log.error("Unexpected failure contacting ML service: {}", ex.getMessage(), ex);
            throw new MlServiceUnavailableException("Unexpected error during clinical risk inference communication.", ex);
        }
    }

    /**
     * Retrieves metadata for all registered models in the saved registry.
     */
    public List<ModelCatalogDTO> getModelCatalog() {
        try {
            return restClient.get()
                    .uri("/api/ml/models")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ModelCatalogDTO>>() {});
        } catch (Exception ex) {
            log.error("Failed to retrieve model catalog from ML service: {}", ex.getMessage());
            throw new MlServiceUnavailableException("Model catalog is currently unavailable.", ex);
        }
    }

    /**
     * Retrieves the population-level validation cohort global SHAP feature importance artifact.
     */
    public Map<String, Object> getGlobalExplanation(String modelName) {
        try {
            return restClient.get()
                    .uri("/api/ml/models/{modelName}/global-explanation", modelName)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("Global explanation artifact not found for model: {}", modelName);
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to retrieve global explanation for model '{}': {}", modelName, ex.getMessage());
            throw new MlServiceUnavailableException("Global explanation is currently unavailable.", ex);
        }
    }
}
