package com.medisphere.risk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Unified clinical risk prediction and SHAP explanation response DTO.
 */
public class RiskPredictionResponseDTO {

    @JsonProperty("task_type")
    private String taskType;

    @JsonProperty("model_name")
    private String modelName;

    @JsonProperty("model_version")
    private String modelVersion;

    @JsonProperty("model_estimated_risk_probability")
    private Double modelEstimatedRiskProbability;

    @JsonProperty("estimated_risk_tier")
    private String estimatedRiskTier;

    @JsonProperty("decision_threshold")
    private Double decisionThreshold;

    @JsonProperty("explanation_space")
    private String explanationSpace;

    @JsonProperty("base_value_log_odds")
    private Double baseValueLogOdds;

    @JsonProperty("total_log_odds")
    private Double totalLogOdds;

    @JsonProperty("feature_attributions")
    private List<FeatureAttributionDTO> featureAttributions;

    @JsonProperty("safety_disclaimer")
    private String safetyDisclaimer;

    public RiskPredictionResponseDTO() {
    }

    public RiskPredictionResponseDTO(String taskType, String modelName, String modelVersion,
                                     Double modelEstimatedRiskProbability, String estimatedRiskTier,
                                     Double decisionThreshold, String explanationSpace,
                                     Double baseValueLogOdds, Double totalLogOdds,
                                     List<FeatureAttributionDTO> featureAttributions, String safetyDisclaimer) {
        this.taskType = taskType;
        this.modelName = modelName;
        this.modelVersion = modelVersion;
        this.modelEstimatedRiskProbability = modelEstimatedRiskProbability;
        this.estimatedRiskTier = estimatedRiskTier;
        this.decisionThreshold = decisionThreshold;
        this.explanationSpace = explanationSpace;
        this.baseValueLogOdds = baseValueLogOdds;
        this.totalLogOdds = totalLogOdds;
        this.featureAttributions = featureAttributions;
        this.safetyDisclaimer = safetyDisclaimer;
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

    public Double getModelEstimatedRiskProbability() {
        return modelEstimatedRiskProbability;
    }

    public void setModelEstimatedRiskProbability(Double modelEstimatedRiskProbability) {
        this.modelEstimatedRiskProbability = modelEstimatedRiskProbability;
    }

    public String getEstimatedRiskTier() {
        return estimatedRiskTier;
    }

    public void setEstimatedRiskTier(String estimatedRiskTier) {
        this.estimatedRiskTier = estimatedRiskTier;
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

    public String getSafetyDisclaimer() {
        return safetyDisclaimer;
    }

    public void setSafetyDisclaimer(String safetyDisclaimer) {
        this.safetyDisclaimer = safetyDisclaimer;
    }
}
