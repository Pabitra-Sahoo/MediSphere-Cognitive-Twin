package com.medisphere.patient.repository;

import com.medisphere.patient.model.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data MongoDB repository for {@link Patient} documents.
 */
@Repository
public interface PatientRepository extends MongoRepository<Patient, String> {

    Optional<Patient> findByMrn(String mrn);

    boolean existsByMrn(String mrn);

    Page<Patient> findByAssignedProviderIdsContaining(String providerId, Pageable pageable);

    @Query("{ $or: [ { 'firstName': { $regex: ?0, $options: 'i' } }, { 'lastName': { $regex: ?0, $options: 'i' } }, { 'mrn': { $regex: ?0, $options: 'i' } } ] }")
    Page<Patient> searchAll(String searchRegex, Pageable pageable);

    @Query("{ 'assignedProviderIds': ?0, $or: [ { 'firstName': { $regex: ?1, $options: 'i' } }, { 'lastName': { $regex: ?1, $options: 'i' } }, { 'mrn': { $regex: ?1, $options: 'i' } } ] }")
    Page<Patient> searchByProvider(String providerId, String searchRegex, Pageable pageable);
}
