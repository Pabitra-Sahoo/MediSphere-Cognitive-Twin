package com.medisphere.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import com.medisphere.audit.model.AuditAction;
import com.medisphere.audit.model.AuditOutcome;
import com.medisphere.audit.service.AuditService;
import com.medisphere.config.FhirConfig;
import com.medisphere.consent.model.Consent;
import com.medisphere.consent.model.ConsentStatus;
import com.medisphere.consent.repository.ConsentRepository;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FHIR Consent Ingestion Tests")
class FhirConsentIngestionTest {

    @Mock
    private FhirResourceRepository fhirResourceRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PatientService patientService;

    @Mock
    private HealthTwinRepository twinRepository;

    @Mock
    private HealthTwinService healthTwinService;

    @Mock
    private LabResultRepository labResultRepository;

    @Mock
    private ConsentRepository consentRepository;

    @Mock
    private AuditService auditService;

    private FhirIngestionService ingestionService;

    @BeforeEach
    void setUp() {
        FhirConfig fhirConfig = new FhirConfig();
        FhirContext fhirContext = fhirConfig.fhirContext();
        FhirValidator fhirValidator = fhirConfig.fhirValidator(fhirContext);
        FhirValidationService validationService = new FhirValidationService(fhirContext, fhirValidator);
        FhirToTwinMapper mapper = new FhirToTwinMapper();

        ingestionService = new FhirIngestionService(
                fhirContext,
                validationService,
                mapper,
                fhirResourceRepository,
                patientRepository,
                patientService,
                twinRepository,
                healthTwinService,
                labResultRepository,
                consentRepository,
                auditService
        );
    }

    @Test
    @DisplayName("Ingesting valid FHIR Consent maps to domain Consent, supersedes previous active, and updates twin metadata")
    void testIngestValidConsent() {
        String consentJson = """
        {
          "resourceType": "Consent",
          "id": "consent-001",
          "status": "active",
          "scope": {
            "coding": [
              {
                "system": "http://terminology.hl7.org/CodeSystem/consentscope",
                "code": "patient-privacy",
                "display": "Privacy Consent"
              }
            ]
          },
          "category": [
            {
              "coding": [
                {
                  "system": "http://terminology.hl7.org/CodeSystem/consentcategorycodes",
                  "code": "hpi",
                  "display": "Protected Health Information"
                }
              ]
            }
          ],
          "patient": { "reference": "Patient/pat-001", "display": "John Doe" },
          "dateTime": "2026-09-04T09:00:00Z",
          "provision": {
            "type": "permit",
            "period": { "end": "2027-09-01T00:00:00Z" },
            "actor": [
              {
                "role": {
                  "coding": [
                    {
                      "system": "http://terminology.hl7.org/CodeSystem/v3-ParticipationType",
                      "code": "PROV",
                      "display": "Healthcare Provider"
                    }
                  ]
                },
                "reference": { "reference": "Practitioner/prov-001", "display": "Dr. Smith" }
              }
            ]
          }
        }
        """;

        Patient patient = new Patient("MRN-10001", "John", "Doe", null, "Male");
        patient.setId("pat-001");
        when(patientRepository.findById("pat-001")).thenReturn(Optional.of(patient));

        Consent existingActive = new Consent();
        existingActive.setId("c-prior");
        existingActive.setStatus(ConsentStatus.GRANTED);
        when(consentRepository.findByPatientIdAndGrantedToAndStatus("pat-001", "prov-001", ConsentStatus.GRANTED))
                .thenReturn(List.of(existingActive));
        when(consentRepository.save(any(Consent.class))).thenAnswer(inv -> {
            Consent c = inv.getArgument(0);
            if (c.getId() == null) c.setId("c-saved-id");
            return c;
        });

        HealthTwin twin = new HealthTwin();
        twin.setPatientId("pat-001");
        when(twinRepository.findByPatientId("pat-001")).thenReturn(Optional.of(twin));

        FhirIngestionResult result = ingestionService.ingest(consentJson);

        assertThat(result.getProcessedResources()).isEqualTo(1);
        assertThat(result.getValidResources()).isEqualTo(1);
        ResourceResult res = result.getResults().get(0);
        assertThat(res.getResourceType()).isEqualTo("Consent");
        assertThat(res.getStatus()).isEqualTo("VALID");
        assertThat(res.getAction()).isEqualTo("MAPPED");

        // Raw FHIR resource saved
        verify(fhirResourceRepository).save(any(FhirResource.class));

        // Previous active consent superseded
        assertThat(existingActive.getStatus()).isEqualTo(ConsentStatus.REVOKED);
        verify(consentRepository).save(existingActive);

        // New domain Consent saved
        ArgumentCaptor<Consent> consentCaptor = ArgumentCaptor.forClass(Consent.class);
        verify(consentRepository, atLeastOnce()).save(consentCaptor.capture());
        Consent newConsent = consentCaptor.getAllValues().stream()
                .filter(c -> c.getStatus() == ConsentStatus.GRANTED)
                .findFirst()
                .orElse(null);
        assertThat(newConsent).isNotNull();
        assertThat(newConsent.getPatientId()).isEqualTo("pat-001");
        assertThat(newConsent.getGrantedTo()).isEqualTo("prov-001");

        // Twin sync metadata updated
        assertThat(twin.getFhirSyncStatus().getLastSyncTime()).isNotNull();
        verify(healthTwinService).recalculateAndSave(eq(twin), eq(patient));

        // Audit events recorded
        verify(auditService).log(eq(AuditAction.FHIR_SYNC), eq("FHIR"), eq("consent-001"),
                eq("pat-001"), any(), eq(AuditOutcome.SUCCESS));
        verify(auditService).log(eq(AuditAction.CONSENT_GRANTED), eq("CONSENT"), eq("c-saved-id"),
                eq("pat-001"), contains("Consent mapped from FHIR"), eq(AuditOutcome.SUCCESS));
    }

    @Test
    @DisplayName("Ingesting FHIR Consent without resolvable actor persists raw resource but does NOT create domain Consent")
    void testIngestConsentWithoutActor() {
        String consentWithoutActor = """
        {
          "resourceType": "Consent",
          "id": "consent-no-actor",
          "status": "active",
          "scope": {
            "coding": [
              {
                "system": "http://terminology.hl7.org/CodeSystem/consentscope",
                "code": "patient-privacy",
                "display": "Privacy Consent"
              }
            ]
          },
          "category": [
            {
              "coding": [
                {
                  "system": "http://terminology.hl7.org/CodeSystem/consentcategorycodes",
                  "code": "hpi",
                  "display": "Protected Health Information"
                }
              ]
            }
          ],
          "patient": { "reference": "Patient/pat-001" },
          "dateTime": "2026-09-04T09:00:00Z",
          "provision": {
            "type": "permit"
          }
        }
        """;

        Patient patient = new Patient("MRN-10001", "John", "Doe", null, "Male");
        patient.setId("pat-001");
        when(patientRepository.findById("pat-001")).thenReturn(Optional.of(patient));

        FhirIngestionResult result = ingestionService.ingest(consentWithoutActor);

        assertThat(result.getProcessedResources()).isEqualTo(1);
        assertThat(result.getValidResources()).isEqualTo(1);
        ResourceResult res = result.getResults().get(0);
        assertThat(res.getStatus()).isEqualTo("VALID");
        assertThat(res.getAction()).isEqualTo("PERSISTED");

        // Raw FHIR resource saved
        verify(fhirResourceRepository).save(any(FhirResource.class));

        // Domain consent NOT saved (no calls to consentRepository.save)
        verify(consentRepository, never()).save(any(Consent.class));
    }
}
