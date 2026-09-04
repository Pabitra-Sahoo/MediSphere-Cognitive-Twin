package com.medisphere.auth.model;

/**
 * System roles for Role-Based Access Control (RBAC).
 *
 * <ul>
 *   <li>{@code ADMIN}: System administrator with full operational and user management access.</li>
 *   <li>{@code PROVIDER}: Healthcare provider with access to assigned patients and clinical data.</li>
 *   <li>{@code PATIENT}: Patient with access strictly limited to their own personal record and twin.</li>
 * </ul>
 */
public enum Role {
    ADMIN,
    PROVIDER,
    PATIENT
}
