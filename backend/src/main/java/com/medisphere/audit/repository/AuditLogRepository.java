package com.medisphere.audit.repository;

import com.medisphere.audit.model.AuditAction;
import com.medisphere.audit.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Spring Data MongoDB repository for immutable {@link AuditLog} documents.
 * No update or delete operations are exposed or invoked by the application.
 */
@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {

    Page<AuditLog> findByPatientId(String patientId, Pageable pageable);

    Page<AuditLog> findByPatientIdAndAction(String patientId, AuditAction action, Pageable pageable);

    Page<AuditLog> findByAction(AuditAction action, Pageable pageable);

    Page<AuditLog> findByUserId(String userId, Pageable pageable);

    List<AuditLog> findByPatientIdOrderByTimestampDesc(String patientId);
}
