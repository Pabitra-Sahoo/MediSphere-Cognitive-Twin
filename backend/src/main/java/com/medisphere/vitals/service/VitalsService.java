package com.medisphere.vitals.service;

import com.medisphere.common.exception.ResourceNotFoundException;
import com.medisphere.patient.model.Patient;
import com.medisphere.patient.repository.PatientRepository;
import com.medisphere.twin.model.HealthTwin;
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
import com.medisphere.vitals.validation.VitalsValidationResult;
import com.medisphere.vitals.validation.VitalsValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Service managing real-time vitals ingestion, idempotency, data-quality validation,
 * persistence, HealthTwin updates, and historical queries.
 */
@Service
public class VitalsService {

    private static final Logger log = LoggerFactory.getLogger(VitalsService.class);

    private final VitalsRepository vitalsRepository;
    private final PatientRepository patientRepository;
    private final HealthTwinRepository healthTwinRepository;
    private final HealthTwinService healthTwinService;
    private final VitalsValidator vitalsValidator;
    private final VitalsProducer vitalsProducer;

    public VitalsService(VitalsRepository vitalsRepository,
                         PatientRepository patientRepository,
                         HealthTwinRepository healthTwinRepository,
                         HealthTwinService healthTwinService,
                         VitalsValidator vitalsValidator,
                         VitalsProducer vitalsProducer) {
        this.vitalsRepository = vitalsRepository;
        this.patientRepository = patientRepository;
        this.healthTwinRepository = healthTwinRepository;
        this.healthTwinService = healthTwinService;
        this.vitalsValidator = vitalsValidator;
        this.vitalsProducer = vitalsProducer;
    }

    /**
     * Processes an incoming vitals event from Kafka or manual ingestion.
     * Enforces idempotency, data-quality validation boundaries, persistence,
     * and HealthTwin updates.
     */
    public Vitals processVitalsEvent(VitalsEventDTO event) {
        if (event == null) {
            log.warn("Received null vitals event, ignoring");
            return null;
        }

        // 1. Ensure eventId exists for idempotency tracking
        if (event.getEventId() == null || event.getEventId().trim().isEmpty()) {
            event.setEventId(UUID.randomUUID().toString());
        }

        // 2. Pre-check idempotency
        if (vitalsRepository.existsByEventId(event.getEventId())) {
            log.info("Duplicate vitals event '{}' detected via pre-check, skipping processing (idempotent)",
                    event.getEventId());
            return vitalsRepository.findByEventId(event.getEventId()).orElse(null);
        }

        Instant recordedAt = event.getRecordedAt() != null ? event.getRecordedAt() : Instant.now();

        Vitals vitals = new Vitals();
        vitals.setEventId(event.getEventId());
        vitals.setPatientId(event.getPatientId());
        vitals.setDeviceId(event.getDeviceId());
        vitals.setHeartRate(event.getHeartRate());
        vitals.setSystolicBP(event.getSystolicBP());
        vitals.setDiastolicBP(event.getDiastolicBP());
        vitals.setOxygenSaturation(event.getOxygenSaturation());
        vitals.setTemperature(event.getTemperature());
        vitals.setRespiratoryRate(event.getRespiratoryRate());
        vitals.setSource(event.getSource() != null ? event.getSource() : VitalsSource.WEARABLE);
        vitals.setRecordedAt(recordedAt);
        vitals.setReceivedAt(Instant.now());
        vitals.setCreatedAt(Instant.now());

        // 3. Verify Patient exists
        Optional<Patient> patientOpt = patientRepository.findById(event.getPatientId());
        if (patientOpt.isEmpty()) {
            log.warn("Vitals received for non-existent patient ID '{}'. Saving as invalid record without twin update.",
                    event.getPatientId());
            vitals.setValid(false);
            vitals.getValidationErrors().add("Patient not found with ID: " + event.getPatientId());
            try {
                return vitalsRepository.save(vitals);
            } catch (DuplicateKeyException e) {
                log.info("Duplicate key detected for unknown patient eventId '{}'. Returning existing record.",
                        vitals.getEventId());
                return vitalsRepository.findByEventId(vitals.getEventId()).orElse(vitals);
            }
        }

        // 4. Data-quality boundary validation
        VitalsValidationResult validationResult = vitalsValidator.validate(
                event.getHeartRate(),
                event.getSystolicBP(),
                event.getDiastolicBP(),
                event.getOxygenSaturation(),
                event.getTemperature(),
                event.getRespiratoryRate()
        );

        vitals.setValid(validationResult.isValid());
        vitals.setValidationErrors(validationResult.getErrors());
        vitals.setValidationWarnings(validationResult.getWarnings());

        // 5. Persist vitals document safely handling any race-condition duplicates
        Vitals saved;
        try {
            saved = vitalsRepository.save(vitals);
        } catch (DuplicateKeyException e) {
            log.info("Duplicate key detected for eventId '{}' during save (race condition). Returning existing record.",
                    vitals.getEventId());
            return vitalsRepository.findByEventId(vitals.getEventId()).orElse(vitals);
        }

        // 6. Update HealthTwin only if valid
        if (validationResult.isValid()) {
            updateHealthTwinVitals(saved, patientOpt.get());
        } else {
            log.warn("Vitals event '{}' failed data-quality validation for patient '{}' (errors: {}). HealthTwin was NOT updated.",
                    event.getEventId(), event.getPatientId(), validationResult.getErrors());
        }

        return saved;
    }

    /**
     * Publishes a simulated vitals measurement to Kafka for asynchronous pipeline ingestion.
     */
    public VitalsEventDTO simulateVitals(VitalsSimulateRequest request) {
        VitalsEventDTO event = new VitalsEventDTO();
        event.setEventId(UUID.randomUUID().toString());
        event.setPatientId(request.getPatientId());
        event.setDeviceId(request.getDeviceId() != null ? request.getDeviceId() : "SIM-001");
        event.setHeartRate(request.getHeartRate());
        event.setSystolicBP(request.getSystolicBP());
        event.setDiastolicBP(request.getDiastolicBP());
        event.setOxygenSaturation(request.getOxygenSaturation());
        event.setTemperature(request.getTemperature());
        event.setRespiratoryRate(request.getRespiratoryRate());
        event.setSource(request.getSource() != null ? request.getSource() : VitalsSource.SIMULATED);
        event.setRecordedAt(request.getRecordedAt() != null ? request.getRecordedAt() : Instant.now());

        vitalsProducer.sendVitals(event);
        return event;
    }

    /**
     * Retrieves historical vitals for a patient with pagination and optional time range filtering.
     */
    public Page<VitalsDTO> getVitalsHistory(String patientId, Instant from, Instant to, Pageable pageable) {
        if (!patientRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Patient not found: " + patientId);
        }

        Page<Vitals> vitalsPage;
        if (from != null && to != null) {
            vitalsPage = vitalsRepository.findByPatientIdAndRecordedAtBetween(patientId, from, to, pageable);
        } else {
            vitalsPage = vitalsRepository.findByPatientId(patientId, pageable);
        }

        return vitalsPage.map(this::toDTO);
    }

    /**
     * Retrieves the latest vital signs measurement for a patient.
     */
    public VitalsDTO getLatestVitals(String patientId) {
        if (!patientRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Patient not found: " + patientId);
        }

        Vitals vitals = vitalsRepository.findFirstByPatientIdAndValidTrueOrderByRecordedAtDesc(patientId)
                .or(() -> vitalsRepository.findFirstByPatientIdOrderByRecordedAtDesc(patientId))
                .orElseThrow(() -> new ResourceNotFoundException("No vitals recorded for patient: " + patientId));

        return toDTO(vitals);
    }

    /**
     * Maps a Vitals domain entity to a VitalsDTO.
     */
    public VitalsDTO toDTO(Vitals v) {
        if (v == null) {
            return null;
        }

        VitalsDTO dto = new VitalsDTO();
        dto.setId(v.getId());
        dto.setPatientId(v.getPatientId());
        dto.setEventId(v.getEventId());
        dto.setDeviceId(v.getDeviceId());
        dto.setHeartRate(v.getHeartRate());
        dto.setSystolicBP(v.getSystolicBP());
        dto.setDiastolicBP(v.getDiastolicBP());
        dto.setOxygenSaturation(v.getOxygenSaturation());
        dto.setTemperature(v.getTemperature());
        dto.setRespiratoryRate(v.getRespiratoryRate());
        dto.setSource(v.getSource());
        dto.setValid(v.isValid());
        dto.setValidationErrors(v.getValidationErrors());
        dto.setValidationWarnings(v.getValidationWarnings());
        dto.setRecordedAt(v.getRecordedAt());
        dto.setReceivedAt(v.getReceivedAt());
        dto.setCreatedAt(v.getCreatedAt());
        return dto;
    }

    private void updateHealthTwinVitals(Vitals vitals, Patient patient) {
        HealthTwin twin = healthTwinRepository.findByPatientId(patient.getId())
                .orElseGet(() -> new HealthTwin(patient.getId()));

        TwinVitals tv = twin.getLatestVitals() != null ? twin.getLatestVitals() : new TwinVitals();

        if (vitals.getHeartRate() != null) {
            tv.setHeartRate(vitals.getHeartRate());
        }
        if (vitals.getSystolicBP() != null) {
            tv.setSystolicBP(vitals.getSystolicBP());
        }
        if (vitals.getDiastolicBP() != null) {
            tv.setDiastolicBP(vitals.getDiastolicBP());
        }
        if (vitals.getOxygenSaturation() != null) {
            tv.setOxygenSaturation(vitals.getOxygenSaturation());
        }
        if (vitals.getTemperature() != null) {
            tv.setTemperature(vitals.getTemperature());
        }
        if (vitals.getRespiratoryRate() != null) {
            tv.setRespiratoryRate(vitals.getRespiratoryRate());
        }
        tv.setTimestamp(vitals.getRecordedAt());

        twin.setLatestVitals(tv);
        healthTwinService.recalculateAndSave(twin, patient);

        log.info("Updated HealthTwin latestVitals and recalculated completeness for patient '{}' (completeness: {}%)",
                patient.getId(), twin.getCompleteness() != null ? twin.getCompleteness().getPercentage() : "N/A");
    }
}
