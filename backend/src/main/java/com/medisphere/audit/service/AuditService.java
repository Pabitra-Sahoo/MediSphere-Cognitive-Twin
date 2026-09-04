package com.medisphere.audit.service;

import com.medisphere.audit.dto.AuditLogDTO;
import com.medisphere.audit.model.AuditAction;
import com.medisphere.audit.model.AuditLog;
import com.medisphere.audit.model.AuditOutcome;
import com.medisphere.audit.repository.AuditLogRepository;
import com.medisphere.auth.model.User;
import com.medisphere.auth.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.List;

/**
 * Service for recording and querying immutable HIPAA-inspired audit logs.
 *
 * <p>Exclusively provides write (append-only) and read capabilities.
 * No update or delete methods exist in this service.</p>
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;

    public AuditService(AuditLogRepository auditLogRepository,
                        UserRepository userRepository,
                        MongoTemplate mongoTemplate) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Explicit base logging method with complete metadata parameters.
     */
    public AuditLog log(String userId, String username, String userRole, AuditAction action,
                        String resourceType, String resourceId, String patientId,
                        String details, AuditOutcome outcome, String ipAddress) {
        String safeDetails = sanitizeDetails(details);
        String clientIp = StringUtils.hasText(ipAddress) ? ipAddress : resolveClientIp();

        AuditLog auditLog = new AuditLog(
                userId,
                username,
                userRole,
                action,
                resourceType,
                resourceId,
                patientId,
                safeDetails,
                outcome,
                clientIp,
                Instant.now()
        );

        AuditLog saved = auditLogRepository.save(auditLog);
        log.debug("AuditLog recorded: action={}, resource={}/{}, outcome={}, user={}",
                action, resourceType, resourceId, outcome, username);
        return saved;
    }

    /**
     * Context-aware logging method extracting the current authenticated user and remote IP.
     */
    public AuditLog log(AuditAction action, String resourceType, String resourceId,
                        String patientId, String details, AuditOutcome outcome) {
        String username = "ANONYMOUS";
        String userRole = "ANONYMOUS";
        String userId = null;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            username = auth.getName();
            userRole = auth.getAuthorities().stream()
                    .map(a -> a.getAuthority().replace("ROLE_", ""))
                    .findFirst()
                    .orElse("USER");

            User user = userRepository.findByUsername(username).orElse(null);
            if (user != null) {
                userId = user.getId();
                if (user.getRole() != null) {
                    userRole = user.getRole().name();
                }
            }
        }

        return log(userId, username, userRole, action, resourceType, resourceId, patientId, details, outcome, resolveClientIp());
    }

    /**
     * Specialized logging method for unauthenticated or pre-authentication events (e.g. login failures).
     */
    public AuditLog logAnonymous(String username, AuditAction action, String resourceType,
                                 String resourceId, String patientId, String details, AuditOutcome outcome) {
        return log(null, StringUtils.hasText(username) ? username : "ANONYMOUS", "ANONYMOUS",
                action, resourceType, resourceId, patientId, details, outcome, resolveClientIp());
    }

    /**
     * Queries audit logs for a specific patient with optional action and date range filters.
     */
    public Page<AuditLogDTO> getPatientAuditLogs(String patientId, AuditAction action,
                                                 Instant from, Instant to, Pageable pageable) {
        Query query = new Query();
        query.addCriteria(Criteria.where("patientId").is(patientId));

        if (action != null) {
            query.addCriteria(Criteria.where("action").is(action));
        }
        applyDateRange(query, from, to);

        long total = mongoTemplate.count(query, AuditLog.class);
        query.with(pageable);
        List<AuditLog> results = mongoTemplate.find(query, AuditLog.class);

        List<AuditLogDTO> dtoList = results.stream().map(AuditLogDTO::new).toList();
        return new PageImpl<>(dtoList, pageable, total);
    }

    /**
     * Queries system-wide audit logs for administrators with optional user, action, and date range filters.
     */
    public Page<AuditLogDTO> getAllAuditLogs(String userId, AuditAction action,
                                             Instant from, Instant to, Pageable pageable) {
        Query query = new Query();

        if (StringUtils.hasText(userId)) {
            query.addCriteria(Criteria.where("userId").is(userId));
        }
        if (action != null) {
            query.addCriteria(Criteria.where("action").is(action));
        }
        applyDateRange(query, from, to);

        long total = mongoTemplate.count(query, AuditLog.class);
        query.with(pageable);
        List<AuditLog> results = mongoTemplate.find(query, AuditLog.class);

        List<AuditLogDTO> dtoList = results.stream().map(AuditLogDTO::new).toList();
        return new PageImpl<>(dtoList, pageable, total);
    }

    private void applyDateRange(Query query, Instant from, Instant to) {
        if (from != null && to != null) {
            query.addCriteria(Criteria.where("timestamp").gte(from).lte(to));
        } else if (from != null) {
            query.addCriteria(Criteria.where("timestamp").gte(from));
        } else if (to != null) {
            query.addCriteria(Criteria.where("timestamp").lte(to));
        }
    }

    /**
     * Resolves the client IP directly using getRemoteAddr() as required for M1.
     */
    private String resolveClientIp() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                if (request != null && StringUtils.hasText(request.getRemoteAddr())) {
                    return request.getRemoteAddr();
                }
            }
        } catch (Exception e) {
            log.trace("Could not extract client remote IP from RequestContext: {}", e.getMessage());
        }
        return "127.0.0.1";
    }

    /**
     * Redacts or sanitizes details to guarantee no credentials or JWT tokens are stored.
     */
    private String sanitizeDetails(String details) {
        if (!StringUtils.hasText(details)) {
            return details;
        }
        String sanitized = details;
        // Redact Bearer tokens if inadvertently passed
        if (sanitized.contains("Bearer ")) {
            sanitized = sanitized.replaceAll("Bearer\\s+[A-Za-z0-9._~+/-]+=*", "Bearer [REDACTED]");
        }
        // Redact password occurrences
        if (sanitized.toLowerCase().contains("password")) {
            sanitized = sanitized.replaceAll("(?i)password[\"':\\s=]+[^\\s,;]+", "password=[REDACTED]");
        }
        return sanitized;
    }
}
