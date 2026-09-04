package com.medisphere.vitals.service;

import com.medisphere.common.exception.ResourceNotFoundException;
import com.medisphere.patient.model.Patient;
import com.medisphere.patient.repository.PatientRepository;
import com.medisphere.twin.model.HealthTwin;
import com.medisphere.twin.model.TwinCompleteness;
import com.medisphere.twin.model.TwinVitals;
import com.medisphere.twin.repository.HealthTwinRepository;
import com.medisphere.twin.service.HealthTwinService;
import com.medisphere.vitals.dto.VitalsDTO;
import com.medisphere.vitals.dto.VitalsEventDTO;
import com.medisphere.vitals.dto.VitalsSimulateRequest;
import com.medisphere.vitals.kafka.VitalsProducer;
import com.medisphere.vitals.model.Vitals;
import com.medisphere.vitals.model.VitalsSource;
import com.medisphere.vitals.repository.VitalsRepository;
import com.medisphere.vitals.validation.VitalsValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VitalsServiceTest {

    @Mock
    private VitalsRepository vitalsRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private HealthTwinRepository healthTwinRepository;

    @Mock
    private HealthTwinService healthTwinService;

    @Mock
    private VitalsProducer vitalsProducer;

    private VitalsValidator vitalsValidator;
    private VitalsService vitalsService;

    private Patient testPatient;
    private HealthTwin testTwin;

    @BeforeEach
    void setUp() {
        vitalsValidator = new VitalsValidator();
        vitalsService = new VitalsService(
                vitalsRepository,
                patientRepository,
                healthTwinRepository,
                healthTwinService,
                vitalsValidator,
                vitalsProducer
        );

        testPatient = new Patient();
        testPatient.setId("pat-001");
        testPatient.setFirstName("John");
        testPatient.setLastName("Doe");

        testTwin = new HealthTwin("pat-001");
        testTwin.setId("twin-001");
        testTwin.setCompleteness(new TwinCompleteness(85.0, List.of(), 20, 17));
    }

    @Test
    @DisplayName("Valid vitals event persists record, updates HealthTwin, and recalculates completeness")
    void testProcessVitalsEvent_Success() {
        VitalsEventDTO event = new VitalsEventDTO();
        event.setEventId("evt-100");
        event.setPatientId("pat-001");
        event.setDeviceId("DEV-01");
        event.setHeartRate(75.0);
        event.setSystolicBP(120.0);
        event.setDiastolicBP(80.0);
        event.setOxygenSaturation(98.0);
        event.setTemperature(36.8);
        event.setRespiratoryRate(16.0);
        event.setSource(VitalsSource.WEARABLE);

        when(vitalsRepository.existsByEventId("evt-100")).thenReturn(false);
        when(patientRepository.findById("pat-001")).thenReturn(Optional.of(testPatient));
        when(vitalsRepository.save(any(Vitals.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(healthTwinRepository.findByPatientId("pat-001")).thenReturn(Optional.of(testTwin));

        Vitals result = vitalsService.processVitalsEvent(event);

        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals("evt-100", result.getEventId());
        assertEquals(75.0, result.getHeartRate());

        // Verify Twin update
        verify(healthTwinService).recalculateAndSave(eq(testTwin), eq(testPatient));
        assertEquals(75.0, testTwin.getLatestVitals().getHeartRate());
        assertEquals(120.0, testTwin.getLatestVitals().getSystolicBP());
        assertEquals(80.0, testTwin.getLatestVitals().getDiastolicBP());
    }

    @Test
    @DisplayName("Duplicate eventId skips processing idempotently (pre-check)")
    void testProcessVitalsEvent_IdempotentPreCheck() {
        VitalsEventDTO event = new VitalsEventDTO();
        event.setEventId("evt-existing");
        event.setPatientId("pat-001");

        Vitals existing = new Vitals();
        existing.setEventId("evt-existing");
        existing.setPatientId("pat-001");

        when(vitalsRepository.existsByEventId("evt-existing")).thenReturn(true);
        when(vitalsRepository.findByEventId("evt-existing")).thenReturn(Optional.of(existing));

        Vitals result = vitalsService.processVitalsEvent(event);

        assertNotNull(result);
        assertEquals("evt-existing", result.getEventId());
        verify(vitalsRepository, never()).save(any());
        verify(healthTwinService, never()).recalculateAndSave(any(), any());
    }

    @Test
    @DisplayName("DuplicateKeyException on save is caught gracefully for idempotency")
    void testProcessVitalsEvent_DuplicateKeyGraceful() {
        VitalsEventDTO event = new VitalsEventDTO();
        event.setEventId("evt-race");
        event.setPatientId("pat-001");
        event.setHeartRate(70.0);

        Vitals existing = new Vitals();
        existing.setEventId("evt-race");

        when(vitalsRepository.existsByEventId("evt-race")).thenReturn(false);
        when(patientRepository.findById("pat-001")).thenReturn(Optional.of(testPatient));
        when(vitalsRepository.save(any(Vitals.class))).thenThrow(new DuplicateKeyException("E11000 duplicate key"));
        when(vitalsRepository.findByEventId("evt-race")).thenReturn(Optional.of(existing));

        Vitals result = vitalsService.processVitalsEvent(event);

        assertNotNull(result);
        assertEquals("evt-race", result.getEventId());
        verify(healthTwinService, never()).recalculateAndSave(any(), any());
    }

    @Test
    @DisplayName("Vitals for unknown patient are saved as invalid without modifying any HealthTwin")
    void testProcessVitalsEvent_UnknownPatient() {
        VitalsEventDTO event = new VitalsEventDTO();
        event.setEventId("evt-unk");
        event.setPatientId("pat-nonexistent");
        event.setHeartRate(72.0);

        when(vitalsRepository.existsByEventId("evt-unk")).thenReturn(false);
        when(patientRepository.findById("pat-nonexistent")).thenReturn(Optional.empty());
        when(vitalsRepository.save(any(Vitals.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Vitals result = vitalsService.processVitalsEvent(event);

        assertNotNull(result);
        assertFalse(result.isValid());
        assertTrue(result.getValidationErrors().get(0).contains("Patient not found"));
        verify(healthTwinRepository, never()).findByPatientId(any());
        verify(healthTwinService, never()).recalculateAndSave(any(), any());
    }

    @Test
    @DisplayName("Out-of-range vitals fail validation and do NOT update HealthTwin")
    void testProcessVitalsEvent_InvalidVitals() {
        VitalsEventDTO event = new VitalsEventDTO();
        event.setEventId("evt-invalid");
        event.setPatientId("pat-001");
        event.setHeartRate(280.0); // Out of boundary
        event.setSystolicBP(70.0);
        event.setDiastolicBP(90.0); // SBP < DBP

        when(vitalsRepository.existsByEventId("evt-invalid")).thenReturn(false);
        when(patientRepository.findById("pat-001")).thenReturn(Optional.of(testPatient));
        when(vitalsRepository.save(any(Vitals.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Vitals result = vitalsService.processVitalsEvent(event);

        assertNotNull(result);
        assertFalse(result.isValid());
        assertEquals(2, result.getValidationErrors().size());
        verify(healthTwinService, never()).recalculateAndSave(any(), any());
    }

    @Test
    @DisplayName("Partial vitals update only present values on HealthTwin")
    void testProcessVitalsEvent_PartialVitals() {
        // Pre-populate twin with existing BP
        testTwin.setLatestVitals(new TwinVitals(null, 130.0, 85.0, null, null, null, Instant.now()));

        VitalsEventDTO event = new VitalsEventDTO();
        event.setEventId("evt-partial");
        event.setPatientId("pat-001");
        event.setHeartRate(82.0);
        event.setOxygenSaturation(97.0);

        when(vitalsRepository.existsByEventId("evt-partial")).thenReturn(false);
        when(patientRepository.findById("pat-001")).thenReturn(Optional.of(testPatient));
        when(vitalsRepository.save(any(Vitals.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(healthTwinRepository.findByPatientId("pat-001")).thenReturn(Optional.of(testTwin));

        Vitals result = vitalsService.processVitalsEvent(event);

        assertTrue(result.isValid());
        assertEquals(82.0, testTwin.getLatestVitals().getHeartRate());
        assertEquals(97.0, testTwin.getLatestVitals().getOxygenSaturation());
        // Existing BP should be preserved
        assertEquals(130.0, testTwin.getLatestVitals().getSystolicBP());
        assertEquals(85.0, testTwin.getLatestVitals().getDiastolicBP());
        verify(healthTwinService).recalculateAndSave(eq(testTwin), eq(testPatient));
    }

    @Test
    @DisplayName("simulateVitals publishes event to KafkaProducer")
    void testSimulateVitals() {
        VitalsSimulateRequest request = new VitalsSimulateRequest();
        request.setPatientId("pat-001");
        request.setHeartRate(75.0);
        request.setSystolicBP(120.0);
        request.setDiastolicBP(80.0);

        VitalsEventDTO result = vitalsService.simulateVitals(request);

        assertNotNull(result);
        assertNotNull(result.getEventId());
        assertEquals("pat-001", result.getPatientId());
        verify(vitalsProducer).sendVitals(result);
    }

    @Test
    @DisplayName("getVitalsHistory throws ResourceNotFoundException for unknown patient")
    void testGetVitalsHistory_PatientNotFound() {
        when(patientRepository.existsById("pat-unknown")).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () ->
                vitalsService.getVitalsHistory("pat-unknown", null, null, PageRequest.of(0, 10)));
    }

    @Test
    @DisplayName("getVitalsHistory returns paginated vitals DTOs")
    void testGetVitalsHistory_Success() {
        when(patientRepository.existsById("pat-001")).thenReturn(true);

        Vitals v1 = new Vitals();
        v1.setId("v-1");
        v1.setPatientId("pat-001");
        v1.setHeartRate(72.0);

        Page<Vitals> page = new PageImpl<>(List.of(v1));
        when(vitalsRepository.findByPatientId(eq("pat-001"), any(PageRequest.class))).thenReturn(page);

        Page<VitalsDTO> result = vitalsService.getVitalsHistory("pat-001", null, null, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals(72.0, result.getContent().get(0).getHeartRate());
    }

    @Test
    @DisplayName("getLatestVitals returns latest valid vital signs")
    void testGetLatestVitals_Success() {
        when(patientRepository.existsById("pat-001")).thenReturn(true);

        Vitals v = new Vitals();
        v.setId("v-latest");
        v.setPatientId("pat-001");
        v.setHeartRate(78.0);
        v.setValid(true);

        when(vitalsRepository.findFirstByPatientIdAndValidTrueOrderByRecordedAtDesc("pat-001"))
                .thenReturn(Optional.of(v));

        VitalsDTO dto = vitalsService.getLatestVitals("pat-001");

        assertNotNull(dto);
        assertEquals(78.0, dto.getHeartRate());
    }

    @Test
    @DisplayName("getLatestVitals throws ResourceNotFoundException if no vitals exist")
    void testGetLatestVitals_NotFound() {
        when(patientRepository.existsById("pat-001")).thenReturn(true);
        when(vitalsRepository.findFirstByPatientIdAndValidTrueOrderByRecordedAtDesc("pat-001"))
                .thenReturn(Optional.empty());
        when(vitalsRepository.findFirstByPatientIdOrderByRecordedAtDesc("pat-001"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> vitalsService.getLatestVitals("pat-001"));
    }
}
