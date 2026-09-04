package com.medisphere.audit.annotation;

import com.medisphere.audit.model.AuditAction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation for declarative AOP auditing of controller view operations.
 *
 * <p>Note: This aspect records audit entries upon successful execution of controller
 * methods. Security denials (e.g. at {@code @PreAuthorize}) are audited directly
 * by {@code SecurityEvaluationService} before method invocation.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

    /**
     * The auditable action type (e.g. VIEW_PATIENT, VIEW_TWIN, VIEW_VITALS, VIEW_LABS).
     */
    AuditAction action();

    /**
     * The logical resource type being accessed (e.g. PATIENT, TWIN, VITALS, LABS).
     */
    String resourceType();
}
