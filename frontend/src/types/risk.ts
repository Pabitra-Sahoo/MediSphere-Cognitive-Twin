/**
 * TypeScript definitions for Clinical Risk Assessment and SHAP Explainability.
 * Strictly mirrors backend RiskPredictionResponseDTO, FeatureAttributionDTO, EncounterContextDTO, ModelCatalogDTO.
 */

export type RiskTaskType = 'CARDIOVASCULAR' | 'DIABETES';

export type RiskTier = 'LOW' | 'MODERATE' | 'HIGH';

export type FeatureDirection = 'INCREASES_RISK' | 'DECREASES_RISK';

export interface FeatureAttribution {
  feature_name: string;
  feature_value: string | number | boolean | null;
  shap_value_log_odds: number;
  direction: FeatureDirection;
  rank?: number;
}

export interface RiskPredictionResponse {
  id?: string;
  patientId?: string;
  task_type: string;
  model_name: string;
  model_version: string;
  model_estimated_risk_probability: number;
  estimated_risk_tier: RiskTier;
  decision_threshold: number;
  explanation_space: string;
  base_value_log_odds: number;
  total_log_odds: number;
  feature_attributions: FeatureAttribution[];
  safety_disclaimer: string;
  evaluatedAt?: string;
}

export interface EncounterContext {
  timeInHospital: number;
  numLabProcedures: number;
  numProcedures: number;
  numMedications: number;
  numberDiagnoses: number;
  maxGluSerum: 'none' | 'norm' | '>200' | '>300' | string;
  a1cResult: 'none' | 'norm' | '>7' | '>8' | string;
  insulin: 'No' | 'Up' | 'Down' | 'Steady' | string;
  diabetesMed: 'Yes' | 'No' | string;
}

export interface ModelCatalogItem {
  model_name: string;
  task_type: string;
  algorithm: string;
  version: string;
  decision_threshold: number;
  feature_contract: string[];
  is_production_default: boolean;
}

export interface GlobalExplanationItem {
  feature: string;
  mean_abs_shap: number;
  rank: number;
  feature_display_name?: string;
}

export interface GlobalExplanation {
  model_name: string;
  task_type: string;
  algorithm: string;
  explanation_space: string;
  cohort_size: number;
  global_attributions: GlobalExplanationItem[];
}
