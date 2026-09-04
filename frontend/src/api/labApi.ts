import { axiosClient } from './axiosClient';
import type { LabResultRecord } from '../types/lab';
import type { PagedResponse } from '../types/patient';

/**
 * Laboratory results API service connecting to `/api/patients/{patientId}/labs`.
 */
export const labApi = {
  async getLabs(
    patientId: string,
    page = 0,
    size = 20
  ): Promise<PagedResponse<LabResultRecord>> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });
    const response = await axiosClient.get<PagedResponse<LabResultRecord>>(
      `/patients/${patientId}/labs?${params.toString()}`
    );
    return response.data;
  },

  async createLab(
    patientId: string,
    labResult: Partial<LabResultRecord>
  ): Promise<LabResultRecord> {
    const response = await axiosClient.post<LabResultRecord>(
      `/patients/${patientId}/labs`,
      labResult
    );
    return response.data;
  },
};
