import { axiosClient } from './axiosClient';
import type { AuditLogDTO } from '../types/audit';
import type { PagedResponse } from '../types/patient';

/**
 * Audit log API service connecting to `/api/patients/{patientId}/audit-logs` and `/api/audit-logs`.
 */
export const auditApi = {
  async getPatientAuditLogs(
    patientId: string,
    page = 0,
    size = 20
  ): Promise<PagedResponse<AuditLogDTO>> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });
    const response = await axiosClient.get<PagedResponse<AuditLogDTO>>(
      `/patients/${patientId}/audit-logs?${params.toString()}`
    );
    return response.data;
  },

  async getAllAuditLogs(page = 0, size = 20): Promise<PagedResponse<AuditLogDTO>> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });
    const response = await axiosClient.get<PagedResponse<AuditLogDTO>>(
      `/audit-logs?${params.toString()}`
    );
    return response.data;
  },
};
