/**
 * Laboratory result domain interface matching backend LabResult.
 */

export interface ReferenceRange {
  low?: number;
  high?: number;
}

export interface LabResultRecord {
  id: string;
  patientId: string;
  testName: string;
  testCode: string;
  value: number;
  unit: string;
  referenceRange?: ReferenceRange;
  status: string;
  performedAt: string;
  reportedAt?: string;
  source?: string;
}
