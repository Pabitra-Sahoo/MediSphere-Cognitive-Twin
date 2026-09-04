import { axiosClient } from './axiosClient';
import type { FhirIngestionResult, FhirResourceDetail, FhirResourceSummary } from '../types/fhir';

/**
 * FHIR API client for ingestion and resource retrieval.
 */
export const fhirApi = {
  /**
   * Ingests a raw FHIR R4 JSON payload (single resource or Bundle).
   * Restricted to ADMIN.
   */
  ingest: async (rawJson: string): Promise<FhirIngestionResult> => {
    const response = await axiosClient.post<FhirIngestionResult>('/fhir/ingest', rawJson, {
      headers: {
        'Content-Type': 'application/fhir+json',
      },
    });
    return response.data;
  },

  /**
   * Retrieves FHIR resources associated with a patient.
   */
  getPatientFhirResources: async (
    patientId: string,
    resourceType?: string
  ): Promise<{ content: FhirResourceSummary[]; totalElements: number }> => {
    const response = await axiosClient.get<{ content: FhirResourceSummary[]; totalElements: number }>(
      `/patients/${patientId}/fhir-resources`,
      {
        params: resourceType ? { resourceType } : {},
      }
    );
    return response.data;
  },

  /**
   * Retrieves full details and raw JSON of a specific FHIR resource.
   */
  getResourceById: async (resourceId: string): Promise<FhirResourceDetail> => {
    const response = await axiosClient.get<FhirResourceDetail>(`/fhir/resources/${resourceId}`);
    return response.data;
  },
};
