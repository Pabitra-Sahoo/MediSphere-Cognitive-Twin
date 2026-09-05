/**
 * Vital signs measurement DTO matching backend VitalsDTO.
 */

export type VitalsSource = 'WEARABLE' | 'MANUAL' | 'FHIR' | 'SIMULATED' | string;

export interface VitalsRecord {
  id: string;
  patientId: string;
  eventId?: string;
  deviceId?: string;
  heartRate?: number;
  systolicBP?: number;
  diastolicBP?: number;
  oxygenSaturation?: number;
  temperature?: number;
  respiratoryRate?: number;
  source?: VitalsSource;
  valid: boolean;
  validationErrors?: string[];
  validationWarnings?: string[];
  recordedAt: string;
  receivedAt?: string;
  createdAt?: string;
}
