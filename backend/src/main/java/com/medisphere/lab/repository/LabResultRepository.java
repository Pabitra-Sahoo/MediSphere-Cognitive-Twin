package com.medisphere.lab.repository;

import com.medisphere.lab.model.LabResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for {@link LabResult}.
 */
@Repository
public interface LabResultRepository extends MongoRepository<LabResult, String> {

    List<LabResult> findByPatientId(String patientId);

    Page<LabResult> findByPatientId(String patientId, Pageable pageable);
}
