/**
 * Audit domain TypeScript interfaces matching backend AuditLogDTO.
 */

export type AuditAction =
  | 'LOGIN'
  | 'LOGOUT'
  | 'VIEW_PATIENT'
  | 'VIEW_TWIN'
  | 'VIEW_VITALS'
  | 'VIEW_LABS'
  | 'INGEST_FHIR'
  | 'GRANT_CONSENT'
  | 'REVOKE_CONSENT'
  | 'ACCESS_DENIED';

export type AuditOutcome = 'SUCCESS' | 'DENIED';

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
