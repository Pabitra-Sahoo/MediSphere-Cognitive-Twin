/**
 * TypeScript types for FHIR R4 resources, ingestion responses, and summaries.
 */

export interface ResourceResult {
  resourceType: string;
  resourceId: string;
  status: 'VALID' | 'INVALID';
  action: 'MAPPED' | 'REJECTED' | 'PERSISTED';
  patientId?: string;
  errors: string[];
}

export interface FhirIngestionResult {
  processedResources: number;
  validResources: number;
  invalidResources: number;
  results: ResourceResult[];
}

export interface FhirResourceSummary {
  id: string;
  resourceType: string;
  resourceId: string;
  validationStatus: 'VALID' | 'INVALID';
  processedAt: string;
}

export interface FhirResourceDetail {
  id: string;
  patientId?: string;
  resourceType: string;
  resourceId: string;
  rawJson: string;
  version: string;
  validationStatus: 'VALID' | 'INVALID';
  validationErrors: string[];
  processedAt: string;
  receivedAt: string;
}
