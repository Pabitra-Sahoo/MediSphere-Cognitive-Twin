/**
 * Digital Health Twin domain TypeScript interfaces.
 */

export interface TwinDemographics {
  age?: number;
  gender?: string;
  bmi?: number;
  height?: number;
  weight?: number;
  bloodType?: string;
}

export interface TwinVitals {
  heartRate?: number;
  systolicBP?: number;
  diastolicBP?: number;
  oxygenSaturation?: number;
  temperature?: number;
  respiratoryRate?: number;
  timestamp?: string;
}

export interface TwinLabs {
  glucose?: number;
  cholesterol?: number;
  hemoglobin?: number;
  creatinine?: number;
  timestamp?: string;
}

export interface TwinFhirSyncStatus {
  lastSyncTime?: string;
  resourceCount: number;
  syncStatus: string;
}

export interface TwinCompleteness {
  percentage: number;
  missingFields: string[];
  totalFields: number;
  populatedFields: number;
  calculatedAt: string;
}

export interface HealthTwin {
  id: string;
  patientId: string;
  demographics: TwinDemographics;
  latestVitals: TwinVitals;
  latestLabs: TwinLabs;
  fhirSyncStatus: TwinFhirSyncStatus;
  completeness: TwinCompleteness;
  riskScores: Record<string, unknown>;
  createdAt?: string;
  updatedAt?: string;
}
