package com.medisphere.consent.repository;

import com.medisphere.consent.model.Consent;
import com.medisphere.consent.model.ConsentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB repository for {@link Consent} documents.
 */
@Repository
public interface ConsentRepository extends MongoRepository<Consent, String> {

    List<Consent> findByPatientId(String patientId);

    Optional<Consent> findFirstByPatientIdAndGrantedToAndStatusOrderByCreatedAtDesc(
            String patientId, String grantedTo, ConsentStatus status);

    List<Consent> findByPatientIdAndGrantedToAndStatus(
            String patientId, String grantedTo, ConsentStatus status);

    List<Consent> findByPatientIdAndGrantedTo(String patientId, String grantedTo);
}
