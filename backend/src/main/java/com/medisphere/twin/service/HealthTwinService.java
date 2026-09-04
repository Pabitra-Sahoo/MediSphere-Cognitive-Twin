package com.medisphere.twin.service;

import com.medisphere.common.exception.ResourceNotFoundException;
import com.medisphere.patient.model.Patient;
import com.medisphere.patient.repository.PatientRepository;
import com.medisphere.twin.dto.HealthTwinDTO;
import com.medisphere.twin.dto.TwinCompletenessDTO;
import com.medisphere.twin.model.HealthTwin;
import com.medisphere.twin.model.TwinCompleteness;
import com.medisphere.twin.model.TwinDemographics;
import com.medisphere.twin.repository.HealthTwinRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

/**
 * Service managing Digital Health Twin entities and the deterministic 20-field completeness calculation.
 */
@Service
public class HealthTwinService {

    private static final Logger log = LoggerFactory.getLogger(HealthTwinService.class);
    public static final int TOTAL_REQUIRED_FIELDS = 20;

    private final HealthTwinRepository twinRepository;
    private final PatientRepository patientRepository;

    public HealthTwinService(HealthTwinRepository twinRepository, PatientRepository patientRepository) {
        this.twinRepository = twinRepository;
        this.patientRepository = patientRepository;
    }

    /**
     * Retrieves the HealthTwin for a specific patient.
     */
    public HealthTwinDTO getTwinByPatientId(String patientId) {
        HealthTwin twin = twinRepository.findByPatientId(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("HealthTwin not found for patient: " + patientId));
        return toDTO(twin);
    }

    /**
     * Retrieves only the twin completeness calculation for a specific patient.
     */
    public TwinCompletenessDTO getCompletenessByPatientId(String patientId) {
        HealthTwin twin = twinRepository.findByPatientId(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("HealthTwin not found for patient: " + patientId));
        return toCompletenessDTO(twin.getCompleteness());
    }

    /**
     * Automatically creates an initial HealthTwin for a newly registered patient.
     */
    public HealthTwin createInitialTwin(Patient patient) {
        HealthTwin twin = new HealthTwin(patient.getId());

        // Sync baseline demographics from patient
        syncDemographicsFromPatient(twin, patient);

        TwinCompleteness completeness = calculateCompleteness(twin, patient);
        twin.setCompleteness(completeness);

        HealthTwin saved = twinRepository.save(twin);
        log.info("Automatically created HealthTwin for patient '{}' (completeness: {}%)",
                patient.getId(), completeness.getPercentage());
        return saved;
    }

    /**
     * Calculates the deterministic 20-field completeness for a HealthTwin and Patient.
     *
     * <p>Evaluates 20 logical fields:
     * <ol>
     *   <li>firstName</li>
     *   <li>lastName</li>
     *   <li>dateOfBirth</li>
     *   <li>gender</li>
     *   <li>height</li>
     *   <li>weight</li>
     *   <li>heartRate</li>
     *   <li>systolicBP</li>
     *   <li>diastolicBP</li>
     *   <li>oxygenSaturation</li>
     *   <li>temperature</li>
     *   <li>respiratoryRate</li>
     *   <li>glucose</li>
     *   <li>cholesterol</li>
     *   <li>hemoglobin</li>
     *   <li>creatinine</li>
     *   <li>mrn</li>
     *   <li>bloodType</li>
     *   <li>emergencyContact (logical object)</li>
     *   <li>fhirSyncStatus (logical object)</li>
     * </ol>
     * </p>
     */
    public TwinCompleteness calculateCompleteness(HealthTwin twin, Patient patient) {
        List<String> missingFields = new ArrayList<>();
        int populated = 0;

        // --- Demographics (6 logical fields) ---
        if (patient != null && isNonBlank(patient.getFirstName())) {
            populated++;
        } else {
            missingFields.add("firstName");
        }

        if (patient != null && isNonBlank(patient.getLastName())) {
            populated++;
        } else {
            missingFields.add("lastName");
        }

        if (patient != null && patient.getDateOfBirth() != null) {
            populated++;
        } else {
            missingFields.add("dateOfBirth");
        }

        if (patient != null && isNonBlank(patient.getGender())) {
            populated++;
        } else {
            missingFields.add("gender");
        }

        if (twin != null && twin.getDemographics() != null && twin.getDemographics().getHeight() != null && twin.getDemographics().getHeight() > 0) {
            populated++;
        } else {
            missingFields.add("height");
        }

        if (twin != null && twin.getDemographics() != null && twin.getDemographics().getWeight() != null && twin.getDemographics().getWeight() > 0) {
            populated++;
        } else {
            missingFields.add("weight");
        }

        // --- Latest Vitals (6 logical fields) ---
        if (twin != null && twin.getLatestVitals() != null && twin.getLatestVitals().getHeartRate() != null) {
            populated++;
        } else {
            missingFields.add("heartRate");
        }

        if (twin != null && twin.getLatestVitals() != null && twin.getLatestVitals().getSystolicBP() != null) {
            populated++;
        } else {
            missingFields.add("systolicBP");
        }

        if (twin != null && twin.getLatestVitals() != null && twin.getLatestVitals().getDiastolicBP() != null) {
            populated++;
        } else {
            missingFields.add("diastolicBP");
        }

        if (twin != null && twin.getLatestVitals() != null && twin.getLatestVitals().getOxygenSaturation() != null) {
            populated++;
        } else {
            missingFields.add("oxygenSaturation");
        }

        if (twin != null && twin.getLatestVitals() != null && twin.getLatestVitals().getTemperature() != null) {
            populated++;
        } else {
            missingFields.add("temperature");
        }

        if (twin != null && twin.getLatestVitals() != null && twin.getLatestVitals().getRespiratoryRate() != null) {
            populated++;
        } else {
            missingFields.add("respiratoryRate");
        }

        // --- Latest Labs (4 logical fields) ---
        if (twin != null && twin.getLatestLabs() != null && twin.getLatestLabs().getGlucose() != null) {
            populated++;
        } else {
            missingFields.add("glucose");
        }

        if (twin != null && twin.getLatestLabs() != null && twin.getLatestLabs().getCholesterol() != null) {
            populated++;
        } else {
            missingFields.add("cholesterol");
        }

        if (twin != null && twin.getLatestLabs() != null && twin.getLatestLabs().getHemoglobin() != null) {
            populated++;
        } else {
            missingFields.add("hemoglobin");
        }

        if (twin != null && twin.getLatestLabs() != null && twin.getLatestLabs().getCreatinine() != null) {
            populated++;
        } else {
            missingFields.add("creatinine");
        }

        // --- Metadata (4 logical fields) ---
        if (patient != null && isNonBlank(patient.getMrn())) {
            populated++;
        } else {
            missingFields.add("mrn");
        }

        if (twin != null && twin.getDemographics() != null && isNonBlank(twin.getDemographics().getBloodType())) {
            populated++;
        } else {
            missingFields.add("bloodType");
        }

        // Logical field: emergencyContact object populated with at least name and phone
        if (patient != null && patient.getEmergencyContact() != null
                && isNonBlank(patient.getEmergencyContact().getName())
                && isNonBlank(patient.getEmergencyContact().getPhone())) {
            populated++;
        } else {
            missingFields.add("emergencyContact");
        }

        // Logical field: fhirSyncStatus object populated with non-null lastSyncTime and syncStatus
        if (twin != null && twin.getFhirSyncStatus() != null
                && twin.getFhirSyncStatus().getLastSyncTime() != null
                && isNonBlank(twin.getFhirSyncStatus().getSyncStatus())) {
            populated++;
        } else {
            missingFields.add("fhirSyncStatus");
        }

        double percentage = Math.round(((double) populated / TOTAL_REQUIRED_FIELDS) * 1000.0) / 10.0;
        return new TwinCompleteness(percentage, missingFields, TOTAL_REQUIRED_FIELDS, populated);
    }

    /**
     * Recalculates and updates the completeness of a HealthTwin after any state change.
     */
    public HealthTwin recalculateAndSave(HealthTwin twin, Patient patient) {
        TwinCompleteness completeness = calculateCompleteness(twin, patient);
        twin.setCompleteness(completeness);
        twin.setUpdatedAt(Instant.now());
        return twinRepository.save(twin);
    }

    /**
     * Synchronizes demographics from Patient document into HealthTwin sub-document.
     */
    public void syncDemographicsFromPatient(HealthTwin twin, Patient patient) {
        if (twin.getDemographics() == null) {
            twin.setDemographics(new TwinDemographics());
        }

        TwinDemographics demo = twin.getDemographics();
        demo.setGender(patient.getGender());

        if (patient.getDateOfBirth() != null) {
            demo.setAge(Period.between(patient.getDateOfBirth(), LocalDate.now()).getYears());
        }

        // Calculate BMI if both height (cm) and weight (kg) are populated
        if (demo.getHeight() != null && demo.getHeight() > 0 && demo.getWeight() != null && demo.getWeight() > 0) {
            double heightInMeters = demo.getHeight() / 100.0;
            double bmi = Math.round((demo.getWeight() / (heightInMeters * heightInMeters)) * 10.0) / 10.0;
            demo.setBmi(bmi);
        }
    }

    public HealthTwinDTO toDTO(HealthTwin twin) {
        HealthTwinDTO dto = new HealthTwinDTO();
        dto.setId(twin.getId());
        dto.setPatientId(twin.getPatientId());
        dto.setDemographics(twin.getDemographics());
        dto.setLatestVitals(twin.getLatestVitals());
        dto.setLatestLabs(twin.getLatestLabs());
        dto.setFhirSyncStatus(twin.getFhirSyncStatus());
        dto.setCompleteness(toCompletenessDTO(twin.getCompleteness()));
        dto.setRiskScores(twin.getRiskScores());
        dto.setCreatedAt(twin.getCreatedAt());
        dto.setUpdatedAt(twin.getUpdatedAt());
        return dto;
    }

    public TwinCompletenessDTO toCompletenessDTO(TwinCompleteness c) {
        if (c == null) {
            return new TwinCompletenessDTO(0.0, List.of(), TOTAL_REQUIRED_FIELDS, 0, Instant.now());
        }
        return new TwinCompletenessDTO(
                c.getPercentage(),
                c.getMissingFields(),
                c.getTotalFields(),
                c.getPopulatedFields(),
                c.getCalculatedAt()
        );
    }

    private boolean isNonBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
