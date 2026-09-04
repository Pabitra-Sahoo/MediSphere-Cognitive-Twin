import { axiosClient } from './axiosClient';
import type { Patient, PatientSummary, PagedResponse } from '../types/patient';
import type { HealthTwin, TwinCompleteness } from '../types/twin';

/**
 * Patient and Digital Health Twin API services connecting to backend `/api/patients` endpoints.
 */
export const patientApi = {
  /**
   * Retrieves a paginated list of patients with twin completeness summary.
   * Scoped by caller role (Admins see all; Providers see assigned only).
   */
  async getPatients(page = 0, size = 20, search?: string): Promise<PagedResponse<PatientSummary>> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });
    if (search && search.trim()) {
      params.append('search', search.trim());
    }
    const response = await axiosClient.get<PagedResponse<PatientSummary>>(`/patients?${params.toString()}`);
    return response.data;
  },

  /**
   * Retrieves full demographics and details for a specific patient.
   */
  async getPatient(patientId: string): Promise<Patient> {
    const response = await axiosClient.get<Patient>(`/patients/${patientId}`);
    return response.data;
  },

  /**
   * Retrieves the Digital Health Twin for a specific patient.
   */
  async getTwin(patientId: string): Promise<HealthTwin> {
    const response = await axiosClient.get<HealthTwin>(`/patients/${patientId}/twin`);
    return response.data;
  },

  /**
   * Retrieves only the twin completeness calculation for a specific patient.
   */
  async getCompleteness(patientId: string): Promise<TwinCompleteness> {
    const response = await axiosClient.get<TwinCompleteness>(`/patients/${patientId}/twin/completeness`);
    return response.data;
  },
};
