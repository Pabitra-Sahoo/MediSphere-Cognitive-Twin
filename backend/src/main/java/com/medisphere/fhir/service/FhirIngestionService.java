package com.medisphere.fhir.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.medisphere.fhir.dto.FhirIngestionResult;
import com.medisphere.fhir.dto.ResourceResult;
import com.medisphere.fhir.mapper.FhirToTwinMapper;
import com.medisphere.fhir.model.FhirResource;
import com.medisphere.fhir.repository.FhirResourceRepository;
import com.medisphere.lab.model.LabResult;
import com.medisphere.lab.repository.LabResultRepository;
import com.medisphere.patient.dto.PatientDTO;
import com.medisphere.patient.model.Patient;
import com.medisphere.patient.repository.PatientRepository;
import com.medisphere.patient.service.PatientService;
import com.medisphere.twin.model.HealthTwin;
import com.medisphere.twin.model.TwinFhirSyncStatus;
import com.medisphere.twin.repository.HealthTwinRepository;
import com.medisphere.twin.service.HealthTwinService;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Consent;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Observation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service orchestrating real FHIR R4 ingestion, validation, persistence,
 * and transformation into Patient, HealthTwin, and LabResult domains.
 */
@Service
public class FhirIngestionService {

    private static final Logger log = LoggerFactory.getLogger(FhirIngestionService.class);

    private final FhirContext fhirContext;
    private final FhirValidationService validationService;
    private final FhirToTwinMapper mapper;
    private final FhirResourceRepository fhirResourceRepository;
    private final PatientRepository patientRepository;
    private final PatientService patientService;
    private final HealthTwinRepository twinRepository;
    private final HealthTwinService healthTwinService;
    private final LabResultRepository labResultRepository;

    public FhirIngestionService(FhirContext fhirContext,
                                FhirValidationService validationService,
                                FhirToTwinMapper mapper,
                                FhirResourceRepository fhirResourceRepository,
                                PatientRepository patientRepository,
                                PatientService patientService,
                                HealthTwinRepository twinRepository,
                                HealthTwinService healthTwinService,
                                LabResultRepository labResultRepository) {
        this.fhirContext = fhirContext;
        this.validationService = validationService;
        this.mapper = mapper;
        this.fhirResourceRepository = fhirResourceRepository;
        this.patientRepository = patientRepository;
        this.patientService = patientService;
        this.twinRepository = twinRepository;
        this.healthTwinService = healthTwinService;
        this.labResultRepository = labResultRepository;
    }

    /**
     * Ingests a FHIR R4 payload (single resource or Bundle), validates each resource,
     * persists raw records, maps valid entities into domain models, and recalculates twin completeness.
     *
     * @param rawJson JSON string representing FHIR resource or Bundle
     * @return Detailed ingestion result
     */
    public FhirIngestionResult ingest(String rawJson) {
        FhirIngestionResult overallResult = new FhirIngestionResult();

        if (!StringUtils.hasText(rawJson)) {
            ResourceResult emptyResult = new ResourceResult(
                    "Unknown", "unknown", "INVALID", "REJECTED",
                    List.of("Request body is empty or blank"));
            overallResult.addResult(emptyResult);
            return overallResult;
        }

        IParser parser = fhirContext.newJsonParser();
        IBaseResource parsedResource;
        try {
            parsedResource = parser.parseResource(rawJson);
        } catch (Exception e) {
            log.warn("Failed to parse incoming payload as FHIR R4 JSON: {}", e.getMessage());
            // Persist rejected raw payload record
            FhirResource rawRecord = new FhirResource(
                    null, "Unknown", "unknown", rawJson, "INVALID",
                    List.of("FHIR R4 JSON parsing error: " + e.getMessage()));
            fhirResourceRepository.save(rawRecord);

            ResourceResult parseErrorResult = new ResourceResult(
                    "Unknown", "unknown", "INVALID", "REJECTED",
                    List.of("FHIR R4 JSON parsing error: " + e.getMessage()));
            overallResult.addResult(parseErrorResult);
            return overallResult;
        }

        // Unpack Bundle entries or process single resource
        List<IBaseResource> resourcesToProcess = new ArrayList<>();
        if (parsedResource instanceof Bundle bundle) {
            log.info("Processing FHIR R4 Bundle with {} entries", bundle.getEntry().size());
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource()) {
                    resourcesToProcess.add(entry.getResource());
                }
            }
        } else {
            resourcesToProcess.add(parsedResource);
        }

        for (IBaseResource resource : resourcesToProcess) {
            ResourceResult resResult = processResource(resource, parser);
            overallResult.addResult(resResult);
        }

        return overallResult;
    }

    private ResourceResult processResource(IBaseResource resource, IParser parser) {
        String resourceType = resource.fhirType();
        String resourceId = resource.getIdElement().hasIdPart() ? resource.getIdElement().getIdPart() : "unknown";
        String encodedJson = parser.encodeResourceToString(resource);

        // 1. Structural & Domain Validation
        FhirValidationService.ValidationOutcome outcome = validationService.validate(resource);

        if (!outcome.isValid()) {
            log.warn("FHIR resource {}/{} failed validation: {}", resourceType, resourceId, outcome.getErrors());
            FhirResource fhirResource = new FhirResource(
                    null, resourceType, resourceId, encodedJson, "INVALID", outcome.getErrors());
            fhirResourceRepository.save(fhirResource);

            return new ResourceResult(resourceType, resourceId, "INVALID", "REJECTED", outcome.getErrors());
        }

        // 2. Resource Mapping & Persistence for VALID resources
        if (resource instanceof org.hl7.fhir.r4.model.Patient fhirPatient) {
            return processValidPatient(fhirPatient, encodedJson);
        } else if (resource instanceof Observation obs) {
            return processValidObservation(obs, encodedJson);
        } else if (resource instanceof DiagnosticReport report) {
            return processValidDiagnosticReport(report, encodedJson);
        } else if (resource instanceof Consent consent) {
            return processValidConsent(consent, encodedJson);
        } else {
            // General valid FHIR resource preservation
            FhirResource fhirResource = new FhirResource(
                    null, resourceType, resourceId, encodedJson, "VALID", List.of());
            fhirResourceRepository.save(fhirResource);
            return new ResourceResult(resourceType, resourceId, "VALID", "PERSISTED", List.of());
        }
    }

    private ResourceResult processValidPatient(org.hl7.fhir.r4.model.Patient fhirPatient, String rawJson) {
        String resourceId = fhirPatient.getIdElement().hasIdPart() ? fhirPatient.getIdElement().getIdPart() : "unknown";
        Optional<String> mrnOpt = mapper.extractMrn(fhirPatient);

        // Match patient: MRN first, then ID
        Patient matchedPatient = null;
        if (mrnOpt.isPresent()) {
            matchedPatient = patientRepository.findByMrn(mrnOpt.get()).orElse(null);
        }
        if (matchedPatient == null && !"unknown".equals(resourceId)) {
            matchedPatient = patientRepository.findById(resourceId).orElse(null);
        }

        if (matchedPatient != null) {
            final Patient patient = matchedPatient;
            // Update existing patient demographics
            mapper.mapPatientDemographics(fhirPatient, patient);
            patientRepository.save(patient);

            HealthTwin twin = twinRepository.findByPatientId(patient.getId())
                    .orElseGet(() -> new HealthTwin(patient.getId()));
            mapper.syncDemographicsToTwin(patient, twin);
            updateFhirSyncMetadata(twin);
            healthTwinService.recalculateAndSave(twin, patient);

            FhirResource fhirResource = new FhirResource(
                    patient.getId(), "Patient", resourceId, rawJson, "VALID", List.of());
            fhirResourceRepository.save(fhirResource);

            return new ResourceResult("Patient", resourceId, "VALID", "MAPPED", patient.getId(), List.of());
        }

        // Unmatched: Create new patient only if MRN is present
        if (mrnOpt.isPresent()) {
            PatientDTO dto = new PatientDTO();
            dto.setMrn(mrnOpt.get());
            Patient tempPatient = new Patient();
            tempPatient.setMrn(mrnOpt.get());
            mapper.mapPatientDemographics(fhirPatient, tempPatient);

            dto.setFirstName(StringUtils.hasText(tempPatient.getFirstName()) ? tempPatient.getFirstName() : "Unknown");
            dto.setLastName(StringUtils.hasText(tempPatient.getLastName()) ? tempPatient.getLastName() : "Unknown");
            dto.setDateOfBirth(tempPatient.getDateOfBirth() != null ? tempPatient.getDateOfBirth() : java.time.LocalDate.of(1990, 1, 1));
            dto.setGender(StringUtils.hasText(tempPatient.getGender()) ? tempPatient.getGender() : "Unknown");
            dto.setEmail(tempPatient.getEmail());
            dto.setPhone(tempPatient.getPhone());

            PatientDTO created = patientService.createPatient(dto);
            HealthTwin twin = twinRepository.findByPatientId(created.getId())
                    .orElseGet(() -> new HealthTwin(created.getId()));
            updateFhirSyncMetadata(twin);
            Patient savedPatient = patientRepository.findById(created.getId()).orElse(tempPatient);
            healthTwinService.recalculateAndSave(twin, savedPatient);

            FhirResource fhirResource = new FhirResource(
                    created.getId(), "Patient", resourceId, rawJson, "VALID", List.of());
            fhirResourceRepository.save(fhirResource);

            return new ResourceResult("Patient", resourceId, "VALID", "MAPPED", created.getId(), List.of());
        }

        // Cannot match and cannot create
        FhirResource fhirResource = new FhirResource(
                null, "Patient", resourceId, rawJson, "VALID", List.of("Cannot match or create patient: missing MRN / identifier"));
        fhirResourceRepository.save(fhirResource);

        return new ResourceResult("Patient", resourceId, "VALID", "REJECTED",
                List.of("Cannot match or create patient: missing MRN / identifier"));
    }

    private ResourceResult processValidObservation(Observation obs, String rawJson) {
        String resourceId = obs.getIdElement().hasIdPart() ? obs.getIdElement().getIdPart() : "unknown";
        Optional<String> subjectIdOpt = mapper.extractSubjectId(obs);

        if (subjectIdOpt.isEmpty()) {
            FhirResource fhirResource = new FhirResource(
                    null, "Observation", resourceId, rawJson, "VALID", List.of("Missing subject reference"));
            fhirResourceRepository.save(fhirResource);
            return new ResourceResult("Observation", resourceId, "VALID", "REJECTED", List.of("Missing subject reference"));
        }

        String subjectRef = subjectIdOpt.get();
        Patient patient = resolvePatient(subjectRef);

        if (patient == null) {
            FhirResource fhirResource = new FhirResource(
                    null, "Observation", resourceId, rawJson, "VALID",
                    List.of("Patient not found for subject reference: " + subjectRef));
            fhirResourceRepository.save(fhirResource);
            return new ResourceResult("Observation", resourceId, "VALID", "REJECTED",
                    List.of("Patient not found for subject reference: " + subjectRef));
        }

        HealthTwin twin = twinRepository.findByPatientId(patient.getId())
                .orElseGet(() -> new HealthTwin(patient.getId()));

        boolean mappedVital = mapper.mapVitalObservation(obs, twin);
        Optional<LabResult> labResultOpt = mapper.mapLabObservation(obs, twin, patient.getId());
        labResultOpt.ifPresent(labResultRepository::save);

        updateFhirSyncMetadata(twin);
        healthTwinService.recalculateAndSave(twin, patient);

        FhirResource fhirResource = new FhirResource(
                patient.getId(), "Observation", resourceId, rawJson, "VALID", List.of());
        fhirResourceRepository.save(fhirResource);

        String action = (mappedVital || labResultOpt.isPresent()) ? "MAPPED" : "PERSISTED";
        return new ResourceResult("Observation", resourceId, "VALID", action, patient.getId(), List.of());
    }

    private ResourceResult processValidDiagnosticReport(DiagnosticReport report, String rawJson) {
        String resourceId = report.getIdElement().hasIdPart() ? report.getIdElement().getIdPart() : "unknown";
        Optional<String> subjectIdOpt = mapper.extractSubjectId(report);

        Patient patient = subjectIdOpt.map(this::resolvePatient).orElse(null);
        String patientId = patient != null ? patient.getId() : null;

        FhirResource fhirResource = new FhirResource(
                patientId, "DiagnosticReport", resourceId, rawJson, "VALID", List.of());
        fhirResourceRepository.save(fhirResource);

        if (patient != null) {
            HealthTwin twin = twinRepository.findByPatientId(patient.getId())
                    .orElseGet(() -> new HealthTwin(patient.getId()));
            updateFhirSyncMetadata(twin);
            healthTwinService.recalculateAndSave(twin, patient);
            return new ResourceResult("DiagnosticReport", resourceId, "VALID", "PERSISTED", patientId, List.of());
        }

        return new ResourceResult("DiagnosticReport", resourceId, "VALID", "PERSISTED", List.of());
    }

    private ResourceResult processValidConsent(Consent consent, String rawJson) {
        String resourceId = consent.getIdElement().hasIdPart() ? consent.getIdElement().getIdPart() : "unknown";
        Optional<String> subjectIdOpt = mapper.extractSubjectId(consent);

        Patient patient = subjectIdOpt.map(this::resolvePatient).orElse(null);
        String patientId = patient != null ? patient.getId() : null;

        FhirResource fhirResource = new FhirResource(
                patientId, "Consent", resourceId, rawJson, "VALID", List.of());
        fhirResourceRepository.save(fhirResource);

        if (patient != null) {
            HealthTwin twin = twinRepository.findByPatientId(patient.getId())
                    .orElseGet(() -> new HealthTwin(patient.getId()));
            updateFhirSyncMetadata(twin);
            healthTwinService.recalculateAndSave(twin, patient);
            return new ResourceResult("Consent", resourceId, "VALID", "PERSISTED", patientId, List.of());
        }

        return new ResourceResult("Consent", resourceId, "VALID", "PERSISTED", List.of());
    }

    private Patient resolvePatient(String ref) {
        if (!StringUtils.hasText(ref)) {
            return null;
        }
        // 1. Match by MRN first
        Patient patient = patientRepository.findByMrn(ref).orElse(null);
        if (patient != null) {
            return patient;
        }
        // 2. Match by ID second
        return patientRepository.findById(ref).orElse(null);
    }

    private void updateFhirSyncMetadata(HealthTwin twin) {
        TwinFhirSyncStatus status = twin.getFhirSyncStatus() != null ? twin.getFhirSyncStatus() : new TwinFhirSyncStatus();
        status.setLastSyncTime(Instant.now());
        status.setSyncStatus("SYNCED");
        status.setResourceCount(status.getResourceCount() + 1);
        twin.setFhirSyncStatus(status);
        twin.setUpdatedAt(Instant.now());
    }
}
