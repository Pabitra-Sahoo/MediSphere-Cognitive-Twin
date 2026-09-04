package com.medisphere.vitals.repository;

import com.medisphere.vitals.model.Vitals;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface VitalsRepository extends MongoRepository<Vitals, String> {

    boolean existsByEventId(String eventId);

    Optional<Vitals> findByEventId(String eventId);

    Page<Vitals> findByPatientId(String patientId, Pageable pageable);

    Page<Vitals> findByPatientIdAndRecordedAtBetween(String patientId, Instant from, Instant to, Pageable pageable);

    Optional<Vitals> findFirstByPatientIdAndValidTrueOrderByRecordedAtDesc(String patientId);

    Optional<Vitals> findFirstByPatientIdOrderByRecordedAtDesc(String patientId);

    List<Vitals> findByPatientIdOrderByRecordedAtDesc(String patientId);

    void deleteByPatientId(String patientId);
}
