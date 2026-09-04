import { axiosClient } from './axiosClient';
import type { VitalsRecord } from '../types/vitals';
import type { PagedResponse } from '../types/patient';

export interface VitalsSimulateRequest {
  patientId: string;
  heartRate: number;
  systolicBP: number;
  diastolicBP: number;
  oxygenSaturation: number;
  temperature: number;
  respiratoryRate: number;
  deviceId?: string;
}

/**
 * Vital signs API service connecting to `/api/patients/{patientId}/vitals` and `/api/vitals/simulate`.
 */
export const vitalsApi = {
  async getLatestVitals(patientId: string): Promise<VitalsRecord> {
    const response = await axiosClient.get<VitalsRecord>(`/patients/${patientId}/vitals/latest`);
    return response.data;
  },

  async getVitalsHistory(
    patientId: string,
    page = 0,
    size = 20
  ): Promise<PagedResponse<VitalsRecord>> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });
    const response = await axiosClient.get<PagedResponse<VitalsRecord>>(
      `/patients/${patientId}/vitals?${params.toString()}`
    );
    return response.data;
  },

  async simulateVitals(request: VitalsSimulateRequest): Promise<{ status: string; message: string; eventId: string }> {
    const response = await axiosClient.post<{ status: string; message: string; eventId: string }>(
      '/vitals/simulate',
      request
    );
    return response.data;
  },
};
