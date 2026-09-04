package com.medisphere.twin.repository;

import com.medisphere.twin.model.HealthTwin;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data MongoDB repository for {@link HealthTwin} documents.
 */
@Repository
public interface HealthTwinRepository extends MongoRepository<HealthTwin, String> {

    Optional<HealthTwin> findByPatientId(String patientId);

    void deleteByPatientId(String patientId);

    boolean existsByPatientId(String patientId);
}
