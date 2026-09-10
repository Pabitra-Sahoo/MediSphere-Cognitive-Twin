import { axiosClient } from './axiosClient';
import type {
  RiskPredictionResponse,
  EncounterContext,
  ModelCatalogItem,
  GlobalExplanation,
} from '../types/risk';

/**
 * Clinical Risk Assessment and SHAP Explainability API service.
 * Connects exclusively through Spring Boot gateway (/api/patients/... and /api/ml/...).
 * NEVER communicates directly with internal FastAPI ML service.
 */
export const riskApi = {
  /**
   * Evaluates 10-year Cardiovascular Disease risk and retrieves local SHAP feature attributions.
   * Clinical features are extracted automatically from the patient's existing HealthTwin.
   *
   * @param patientId ID of the target patient
   * @param benchmarkModel Optional research model override (ADMIN only)
   */
  async evaluateCardiovascularRisk(
    patientId: string,
    benchmarkModel?: string
  ): Promise<RiskPredictionResponse> {
    const params = benchmarkModel ? `?benchmarkModel=${encodeURIComponent(benchmarkModel)}` : '';
    const response = await axiosClient.post<RiskPredictionResponse>(
      `/patients/${patientId}/risk/cardiovascular${params}`
    );
    return response.data;
  },

  /**
   * Evaluates Diabetes Inpatient Complications risk and retrieves local SHAP feature attributions.
   * Merges patient demographics with the acute encounter context.
   *
   * @param patientId ID of the target patient
   * @param context Explicit acute encounter measurements (vitals, labs, medications)
   * @param benchmarkModel Optional research model override (ADMIN only)
   */
  async evaluateDiabetesRisk(
    patientId: string,
    context: EncounterContext,
    benchmarkModel?: string
  ): Promise<RiskPredictionResponse> {
    const params = benchmarkModel ? `?benchmarkModel=${encodeURIComponent(benchmarkModel)}` : '';
    const response = await axiosClient.post<RiskPredictionResponse>(
      `/patients/${patientId}/risk/diabetes${params}`,
      context
    );
    return response.data;
  },

  /**
   * Retrieves the chronological history of risk assessments and archived SHAP attributions for a patient.
   */
  async getRiskHistory(patientId: string): Promise<RiskPredictionResponse[]> {
    const response = await axiosClient.get<RiskPredictionResponse[]>(
      `/patients/${patientId}/risk/history`
    );
    return response.data;
  },

  /**
   * Lists available clinical models and metadata from the catalog.
   * Restricted to PROVIDER and ADMIN roles.
   */
  async getModelCatalog(): Promise<ModelCatalogItem[]> {
    const response = await axiosClient.get<ModelCatalogItem[]>('/ml/models');
    return response.data;
  },

  /**
   * Retrieves precomputed population-level validation SHAP importance for a given model.
   */
  async getGlobalExplanation(modelName: string): Promise<GlobalExplanation> {
    const response = await axiosClient.get<GlobalExplanation>(
      `/ml/models/${encodeURIComponent(modelName)}/global-explanation`
    );
    return response.data;
  },
};
