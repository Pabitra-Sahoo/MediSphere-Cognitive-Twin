package com.medisphere.risk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * DTO representing a registered clinical model's metadata.
 */
public class ModelCatalogDTO {

    @JsonProperty("model_name")
    private String modelName;

    @JsonProperty("task_type")
    private String taskType;

    private String algorithm;
    private String version;

    @JsonProperty("decision_threshold")
    private Double decisionThreshold;

    @JsonProperty("feature_contract")
    private List<String> featureContract;

    @JsonProperty("is_production_default")
    private Boolean isProductionDefault;

    public ModelCatalogDTO() {
    }

    public ModelCatalogDTO(String modelName, String taskType, String algorithm, String version,
                           Double decisionThreshold, List<String> featureContract, Boolean isProductionDefault) {
        this.modelName = modelName;
        this.taskType = taskType;
        this.algorithm = algorithm;
        this.version = version;
        this.decisionThreshold = decisionThreshold;
        this.featureContract = featureContract;
        this.isProductionDefault = isProductionDefault;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Double getDecisionThreshold() {
        return decisionThreshold;
    }

    public void setDecisionThreshold(Double decisionThreshold) {
        this.decisionThreshold = decisionThreshold;
    }

    public List<String> getFeatureContract() {
        return featureContract;
    }

    public void setFeatureContract(List<String> featureContract) {
        this.featureContract = featureContract;
    }

    public Boolean getIsProductionDefault() {
        return isProductionDefault;
    }

    public void setIsProductionDefault(Boolean isProductionDefault) {
        this.isProductionDefault = isProductionDefault;
    }
}
