/**
 * Vital signs measurement DTO matching backend VitalsDTO.
 */

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
  source: 'WEARABLE' | 'MANUAL' | 'FHIR_SIMULATION';
  valid: boolean;
  validationErrors?: string[];
  validationWarnings?: string[];
  recordedAt: string;
  receivedAt?: string;
  createdAt?: string;
}
