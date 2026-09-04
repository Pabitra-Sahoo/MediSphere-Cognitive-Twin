/**
 * Consent domain TypeScript interfaces matching backend ConsentDTO and requests.
 */

export type ConsentStatus = 'ACTIVE' | 'REVOKED' | 'EXPIRED';

export interface ConsentDTO {
  id: string;
  patientId: string;
  grantedTo: string;
  grantedToName: string;
  scope: string;
  status: ConsentStatus;
  grantedAt: string;
  expiresAt?: string;
  revokedAt?: string;
  reason?: string;
}

export interface ConsentCreateRequest {
  grantedTo: string;
  scope?: string;
  expiresAt?: string;
  reason?: string;
}

export interface ConsentVerifyResponse {
  patientId: string;
  providerId: string;
  hasConsent: boolean;
  reason: string;
  checkedAt: string;
}
