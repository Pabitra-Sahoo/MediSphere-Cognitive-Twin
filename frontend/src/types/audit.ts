/**
 * Audit domain TypeScript interfaces matching backend AuditLogDTO.
 */

export type AuditAction =
  | 'USER_LOGIN'
  | 'USER_LOGIN_FAILED'
  | 'VIEW_PATIENT'
  | 'VIEW_TWIN'
  | 'VIEW_VITALS'
  | 'VIEW_LABS'
  | 'FHIR_SYNC'
  | 'FHIR_VALIDATION_FAIL'
  | 'CONSENT_GRANTED'
  | 'CONSENT_REVOKED'
  | 'CONSENT_VERIFIED'
  | 'ACCESS_DENIED'
  | 'VITALS_RECEIVED'
  | 'VITALS_REJECTED'
  | string;

export type AuditOutcome = 'SUCCESS' | 'FAILURE' | 'DENIED' | string;

export interface AuditLogDTO {
  id: string;
  userId?: string;
  username: string;
  userRole?: string;
  action: AuditAction;
  resourceType?: string;
  resourceId?: string;
  patientId?: string;
  details?: string;
  outcome: AuditOutcome;
  ipAddress?: string;
  timestamp: string;
}
