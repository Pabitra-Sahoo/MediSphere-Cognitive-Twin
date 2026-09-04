package com.medisphere.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import com.medisphere.config.FhirConfig;
import com.medisphere.fhir.dto.FhirIngestionResult;
import com.medisphere.fhir.dto.ResourceResult;
import com.medisphere.fhir.mapper.FhirToTwinMapper;
import com.medisphere.fhir.model.FhirResource;
import com.medisphere.fhir.repository.FhirResourceRepository;
import com.medisphere.fhir.service.FhirIngestionService;
import com.medisphere.fhir.service.FhirValidationService;
import com.medisphere.lab.repository.LabResultRepository;
import com.medisphere.patient.model.Patient;
import com.medisphere.patient.repository.PatientRepository;
import com.medisphere.patient.service.PatientService;
import com.medisphere.twin.model.HealthTwin;
import com.medisphere.twin.repository.HealthTwinRepository;
import com.medisphere.twin.service.HealthTwinService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FhirIngestionService Tests")
class FhirIngestionServiceTest {

    @Mock
    private FhirResourceRepository fhirResourceRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PatientService patientService;

    @Mock
    private HealthTwinRepository twinRepository;

    @Mock
    private LabResultRepository labResultRepository;

    private HealthTwinService healthTwinService;
    private FhirIngestionService ingestionService;

    @BeforeEach
    void setUp() {
        FhirConfig config = new FhirConfig();
        FhirContext fhirContext = config.fhirContext();
        FhirValidator fhirValidator = config.fhirValidator(fhirContext);
        FhirValidationService validationService = new FhirValidationService(fhirContext, fhirValidator);
        FhirToTwinMapper mapper = new FhirToTwinMapper();

        healthTwinService = new HealthTwinService(twinRepository, patientRepository);

        ingestionService = new FhirIngestionService(
                fhirContext,
                validationService,
                mapper,
                fhirResourceRepository,
                patientRepository,
                patientService,
                twinRepository,
                healthTwinService,
                labResultRepository
        );
    }

    @Test
    @DisplayName("Ingests valid Patient Bundle matching existing pat-002 (Jane Roe)")
    void testIngestPatientBundleMatchingExistingPatient() {
        Patient existingJane = new Patient("MRN-10002", "Jane", "Roe", LocalDate.of(1988, 7, 14), "Female");
        existingJane.setId("pat-002");
        when(patientRepository.findByMrn("MRN-10002")).thenReturn(Optional.of(existingJane));
        when(patientRepository.findById("pat-002")).thenReturn(Optional.of(existingJane));
        when(patientRepository.save(any(Patient.class))).thenReturn(existingJane);

        HealthTwin existingTwin = new HealthTwin("pat-002");
        when(twinRepository.findByPatientId("pat-002")).thenReturn(Optional.of(existingTwin));
        when(twinRepository.save(any(HealthTwin.class))).thenReturn(existingTwin);

        String bundleJson = """
        {
          "resourceType": "Bundle",
          "type": "collection",
          "entry": [
            {
              "resource": {
                "resourceType": "Patient",
                "id": "pat-002",
                "identifier": [{"system": "http://medisphere.com/mrn", "value": "MRN-10002"}],
                "name": [{"family": "Roe", "given": ["Jane"]}],
                "gender": "female",
                "birthDate": "1988-07-14"
              }
            },
            {
              "resource": {
                "resourceType": "Observation",
                "id": "obs-hr",
                "status": "final",
                "code": {"coding": [{"system": "http://loinc.org", "code": "8867-4", "display": "Heart rate"}]},
                "subject": {"reference": "Patient/pat-002"},
                "valueQuantity": {"value": 68.0, "unit": "/min"}
              }
            }
          ]
        }
        """;

        FhirIngestionResult result = ingestionService.ingest(bundleJson);

        assertNotNull(result);
        assertEquals(2, result.getProcessedResources());
        assertEquals(2, result.getValidResources());
        assertEquals(0, result.getInvalidResources());

        ResourceResult patientResult = result.getResults().get(0);
        assertEquals("Patient", patientResult.getResourceType());
        assertEquals("VALID", patientResult.getStatus());
        assertEquals("MAPPED", patientResult.getAction());
        assertEquals("pat-002", patientResult.getPatientId());

        ResourceResult obsResult = result.getResults().get(1);
        assertEquals("Observation", obsResult.getResourceType());
        assertEquals("VALID", obsResult.getStatus());
        assertEquals("MAPPED", obsResult.getAction());

        verify(fhirResourceRepository, org.mockito.Mockito.atLeastOnce()).save(any(FhirResource.class));
        verify(patientRepository).save(any(Patient.class));
        verify(twinRepository, org.mockito.Mockito.atLeastOnce()).save(any(HealthTwin.class));
    }

    @Test
    @DisplayName("Ingests valid Observation and maps to latestVitals and recalculates completeness")
    void testIngestVitalsObservation() {
        Patient john = new Patient("MRN-10001", "John", "Doe", LocalDate.of(1980, 5, 12), "Male");
        john.setId("pat-001");
        when(patientRepository.findById("pat-001")).thenReturn(Optional.of(john));

        HealthTwin johnTwin = new HealthTwin("pat-001");
        when(twinRepository.findByPatientId("pat-001")).thenReturn(Optional.of(johnTwin));
        when(twinRepository.save(any(HealthTwin.class))).thenReturn(johnTwin);

        String obsJson = """
        {
          "resourceType": "Observation",
          "id": "obs-john-hr",
          "status": "final",
          "code": {"coding": [{"system": "http://loinc.org", "code": "8867-4", "display": "Heart rate"}]},
          "subject": {"reference": "Patient/pat-001"},
          "valueQuantity": {"value": 75.0, "unit": "/min"}
        }
        """;

        FhirIngestionResult result = ingestionService.ingest(obsJson);

        assertNotNull(result);
        assertEquals(1, result.getProcessedResources());
        assertEquals(1, result.getValidResources());
        assertEquals(0, result.getInvalidResources());
        assertEquals("MAPPED", result.getResults().get(0).getAction());
        assertEquals(75.0, johnTwin.getLatestVitals().getHeartRate());
        assertEquals("SYNCED", johnTwin.getFhirSyncStatus().getSyncStatus());
    }

    @Test
    @DisplayName("Rejects invalid Observation missing mandatory code and status, preserves twin")
    void testIngestInvalidObservation() {
        String invalidJson = """
        {
          "resourceType": "Observation",
          "id": "obs-invalid",
          "subject": {"reference": "Patient/pat-001"},
          "valueQuantity": {"value": 150.0}
        }
        """;

        FhirIngestionResult result = ingestionService.ingest(invalidJson);

        assertNotNull(result);
        assertEquals(1, result.getProcessedResources());
        assertEquals(0, result.getValidResources());
        assertEquals(1, result.getInvalidResources());

        ResourceResult res = result.getResults().get(0);
        assertEquals("Observation", res.getResourceType());
        assertEquals("INVALID", res.getStatus());
        assertEquals("REJECTED", res.getAction());
        assertFalse(res.getErrors().isEmpty());

        // Confirms invalid record was saved to fhir_resources collection with INVALID status
        verify(fhirResourceRepository).save(any(FhirResource.class));
    }

    @Test
    @DisplayName("Handles malformed JSON gracefully with HTTP/parse error result")
    void testIngestMalformedJson() {
        String malformedJson = "{ invalid json payload ";

        FhirIngestionResult result = ingestionService.ingest(malformedJson);

        assertNotNull(result);
        assertEquals(1, result.getProcessedResources());
        assertEquals(0, result.getValidResources());
        assertEquals(1, result.getInvalidResources());
        assertEquals("INVALID", result.getResults().get(0).getStatus());
        assertEquals("REJECTED", result.getResults().get(0).getAction());
        assertTrue(result.getResults().get(0).getErrors().get(0).contains("parsing error"));
    }
}
