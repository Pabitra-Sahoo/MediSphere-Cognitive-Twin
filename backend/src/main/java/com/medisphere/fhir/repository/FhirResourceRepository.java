package com.medisphere.fhir.repository;

import com.medisphere.fhir.model.FhirResource;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB repository for {@link FhirResource}.
 */
@Repository
public interface FhirResourceRepository extends MongoRepository<FhirResource, String> {

    List<FhirResource> findByPatientId(String patientId, Sort sort);

    List<FhirResource> findByPatientIdAndResourceType(String patientId, String resourceType, Sort sort);

    Optional<FhirResource> findByResourceId(String resourceId);
}
