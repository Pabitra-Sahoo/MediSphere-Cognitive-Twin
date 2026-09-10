package com.medisphere.risk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing an individual feature's SHAP contribution in log-odds space.
 */
public class FeatureAttributionDTO {

    @JsonProperty("feature_name")
    private String featureName;

    @JsonProperty("feature_value")
    private Object featureValue;

    @JsonProperty("shap_value_log_odds")
    private Double shapValueLogOdds;

    private String direction;
    private Integer rank;

    public FeatureAttributionDTO() {
    }

    public FeatureAttributionDTO(String featureName, Object featureValue, Double shapValueLogOdds, String direction, Integer rank) {
        this.featureName = featureName;
        this.featureValue = featureValue;
        this.shapValueLogOdds = shapValueLogOdds;
        this.direction = direction;
        this.rank = rank;
    }

    public FeatureAttributionDTO(String featureName, Object featureValue, Double shapValueLogOdds, String direction) {
        this(featureName, featureValue, shapValueLogOdds, direction, 1);
    }

    public String getFeatureName() {
        return featureName;
    }

    public void setFeatureName(String featureName) {
        this.featureName = featureName;
    }

    public Object getFeatureValue() {
        return featureValue;
    }

    public void setFeatureValue(Object featureValue) {
        this.featureValue = featureValue;
    }

    public Double getShapValueLogOdds() {
        return shapValueLogOdds;
    }

    public void setShapValueLogOdds(Double shapValueLogOdds) {
        this.shapValueLogOdds = shapValueLogOdds;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }
}
