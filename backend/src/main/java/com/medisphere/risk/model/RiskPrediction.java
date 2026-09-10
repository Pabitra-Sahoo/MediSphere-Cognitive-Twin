package com.medisphere.risk.model;

import com.medisphere.risk.dto.FeatureAttributionDTO;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Historical record of an AI clinical risk evaluation and its local SHAP feature attributions.
 * Persisted in the {@code risk_predictions} MongoDB collection for regulatory auditability and accountability.
 * Contains ZERO patient PII/PHI (no names, MRNs, phone numbers, or addresses).
 */
@Document(collection = "risk_predictions")
public class RiskPrediction {

    @Id
    private String id;

    @Indexed
    private String patientId;

    private String taskType;
    private String modelName;
    private String modelVersion;
    private Double riskProbability;
    private String riskTier;
    private Double decisionThreshold;
    private String explanationSpace;
    private Double baseValueLogOdds;
    private Double totalLogOdds;

    /**
     * Local feature attributions explaining the model decision at inference time.
     * Retained for clinical accountability and historical review.
     */
    private List<FeatureAttributionDTO> featureAttributions = new ArrayList<>();

    private String disclaimer;
    private Instant evaluatedAt;

    public RiskPrediction() {
        this.evaluatedAt = Instant.now();
    }

    public RiskPrediction(String patientId, String taskType, String modelName, String modelVersion,
                          Double riskProbability, String riskTier, Double decisionThreshold,
                          String explanationSpace, Double baseValueLogOdds, Double totalLogOdds,
                          List<FeatureAttributionDTO> featureAttributions, String disclaimer) {
        this.patientId = patientId;
        this.taskType = taskType;
        this.modelName = modelName;
        this.modelVersion = modelVersion;
        this.riskProbability = riskProbability;
        this.riskTier = riskTier;
        this.decisionThreshold = decisionThreshold;
        this.explanationSpace = explanationSpace;
        this.baseValueLogOdds = baseValueLogOdds;
        this.totalLogOdds = totalLogOdds;
        this.featureAttributions = featureAttributions != null ? featureAttributions : new ArrayList<>();
        this.disclaimer = disclaimer;
        this.evaluatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public Double getRiskProbability() {
        return riskProbability;
    }

    public void setRiskProbability(Double riskProbability) {
        this.riskProbability = riskProbability;
    }

    public String getRiskTier() {
        return riskTier;
    }

    public void setRiskTier(String riskTier) {
        this.riskTier = riskTier;
    }

    public Double getDecisionThreshold() {
        return decisionThreshold;
    }

    public void setDecisionThreshold(Double decisionThreshold) {
        this.decisionThreshold = decisionThreshold;
    }

    public String getExplanationSpace() {
        return explanationSpace;
    }

    public void setExplanationSpace(String explanationSpace) {
        this.explanationSpace = explanationSpace;
    }

    public Double getBaseValueLogOdds() {
        return baseValueLogOdds;
    }

    public void setBaseValueLogOdds(Double baseValueLogOdds) {
        this.baseValueLogOdds = baseValueLogOdds;
    }

    public Double getTotalLogOdds() {
        return totalLogOdds;
    }

    public void setTotalLogOdds(Double totalLogOdds) {
        this.totalLogOdds = totalLogOdds;
    }

    public List<FeatureAttributionDTO> getFeatureAttributions() {
        return featureAttributions;
    }

    public void setFeatureAttributions(List<FeatureAttributionDTO> featureAttributions) {
        this.featureAttributions = featureAttributions;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }

    public Instant getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(Instant evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }
}
