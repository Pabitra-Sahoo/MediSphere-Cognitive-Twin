package com.medisphere.audit.model;

/**
 * Enumeration of auditable actions within the MediSphere platform.
 * Follows HIPAA-inspired security logging practices.
 */
public enum AuditAction {
    USER_LOGIN,
    USER_LOGIN_FAILED,
    VIEW_PATIENT,
    VIEW_TWIN,
    VIEW_VITALS,
    VIEW_LABS,
    FHIR_SYNC,
    FHIR_VALIDATION_FAIL,
    CONSENT_GRANTED,
    CONSENT_REVOKED,
    CONSENT_VERIFIED,
    ACCESS_DENIED,
    VITALS_RECEIVED,
    VITALS_REJECTED
}
