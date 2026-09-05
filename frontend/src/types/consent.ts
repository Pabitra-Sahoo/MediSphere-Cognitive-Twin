/**
 * Consent domain TypeScript interfaces matching backend ConsentDTO and requests.
 */

export type ConsentStatus = 'GRANTED' | 'REVOKED' | 'PENDING' | string;

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
  hasConsent: boolean;
  consentId?: string | null;
  scope?: string | null;
  expiresAt?: string | null;
  providerId?: string;
  reason?: string;
  checkedAt?: string;
}
