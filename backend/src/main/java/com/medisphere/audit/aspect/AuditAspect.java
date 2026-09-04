package com.medisphere.audit.aspect;

import com.medisphere.audit.annotation.Audited;
import com.medisphere.audit.model.AuditOutcome;
import com.medisphere.audit.service.AuditService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Spring AOP Aspect that captures successful reads on controller methods annotated with {@link Audited}.
 *
 * <p>Crucial M1 Architecture Note:
 * Method-level security rejections (via {@code @PreAuthorize}) occur prior to entering the controller
 * method and are directly audited by {@code SecurityEvaluationService}. This aspect is executed solely
 * for successful controller method executions.</p>
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditService auditService;

    public AuditAspect(AuditService auditService) {
        this.auditService = auditService;
    }

    @Around("@annotation(audited)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        Object result = joinPoint.proceed();

        try {
            String patientId = extractPatientId(joinPoint);
            String resourceId = StringUtils.hasText(patientId) ? patientId : "unknown";
            String details = String.format("Successfully accessed %s resource (operation: %s)",
                    audited.resourceType(), audited.action());

            auditService.log(
                    audited.action(),
                    audited.resourceType(),
                    resourceId,
                    patientId,
                    details,
                    AuditOutcome.SUCCESS
            );
        } catch (Exception e) {
            log.warn("Failed to record audit log in AuditAspect for {}: {}", audited.action(), e.getMessage());
        }

        return result;
    }

    private String extractPatientId(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameters.length; i++) {
            String paramName = parameters[i].getName();
            if ("patientId".equalsIgnoreCase(paramName) || "id".equalsIgnoreCase(paramName)) {
                if (args[i] != null) {
                    return args[i].toString();
                }
            }
        }

        // Fallback: check if first argument is a non-empty string ID
        if (args.length > 0 && args[0] instanceof String str && StringUtils.hasText(str)) {
            return str;
        }

        return null;
    }
}
