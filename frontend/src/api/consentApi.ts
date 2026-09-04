import { axiosClient } from './axiosClient';
import type { ConsentDTO, ConsentCreateRequest, ConsentVerifyResponse } from '../types/consent';

/**
 * Consent management API service connecting to `/api/patients/{patientId}/consents`.
 */
export const consentApi = {
  async getConsents(patientId: string): Promise<ConsentDTO[]> {
    const response = await axiosClient.get<ConsentDTO[]>(`/patients/${patientId}/consents`);
    return response.data;
  },

  async grantConsent(patientId: string, request: ConsentCreateRequest): Promise<ConsentDTO> {
    const response = await axiosClient.post<ConsentDTO>(`/patients/${patientId}/consents`, request);
    return response.data;
  },

  async revokeConsent(patientId: string, consentId: string): Promise<ConsentDTO> {
    const response = await axiosClient.put<ConsentDTO>(`/patients/${patientId}/consents/${consentId}/revoke`);
    return response.data;
  },

  async verifyConsent(patientId: string, providerId?: string): Promise<ConsentVerifyResponse> {
    const url = providerId
      ? `/patients/${patientId}/consents/verify?providerId=${encodeURIComponent(providerId)}`
      : `/patients/${patientId}/consents/verify`;
    const response = await axiosClient.get<ConsentVerifyResponse>(url);
    return response.data;
  },
};
