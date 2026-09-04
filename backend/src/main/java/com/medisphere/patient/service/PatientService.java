package com.medisphere.patient.service;

import com.medisphere.auth.model.Role;
import com.medisphere.auth.model.User;
import com.medisphere.auth.repository.UserRepository;
import com.medisphere.common.dto.PagedResponse;
import com.medisphere.common.exception.ResourceNotFoundException;
import com.medisphere.patient.dto.AddressDTO;
import com.medisphere.patient.dto.EmergencyContactDTO;
import com.medisphere.patient.dto.InsuranceInfoDTO;
import com.medisphere.patient.dto.PatientDTO;
import com.medisphere.patient.dto.PatientSummaryDTO;
import com.medisphere.patient.model.Address;
import com.medisphere.patient.model.EmergencyContact;
import com.medisphere.patient.model.InsuranceInfo;
import com.medisphere.patient.model.Patient;
import com.medisphere.patient.repository.PatientRepository;
import com.medisphere.twin.model.HealthTwin;
import com.medisphere.twin.repository.HealthTwinRepository;
import com.medisphere.twin.service.HealthTwinService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;

/**
 * Service managing Patient records, provider assignment, and automatic HealthTwin creation.
 */
@Service
public class PatientService {

    private static final Logger log = LoggerFactory.getLogger(PatientService.class);

    private final PatientRepository patientRepository;
    private final HealthTwinRepository twinRepository;
    private final HealthTwinService healthTwinService;
    private final UserRepository userRepository;

    public PatientService(PatientRepository patientRepository,
                          HealthTwinRepository twinRepository,
                          HealthTwinService healthTwinService,
                          UserRepository userRepository) {
        this.patientRepository = patientRepository;
        this.twinRepository = twinRepository;
        this.healthTwinService = healthTwinService;
        this.userRepository = userRepository;
    }

    /**
     * Retrieves a paginated list of patients with twin completeness summary.
     * Providers see only assigned patients; Admins see all patients.
     */
    public PagedResponse<PatientSummaryDTO> getPatients(String username, String search, Pageable pageable) {
        User caller = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));

        Page<Patient> page;
        boolean hasSearch = StringUtils.hasText(search);

        if (caller.getRole() == Role.ADMIN) {
            page = hasSearch
                    ? patientRepository.searchAll(search.trim(), pageable)
                    : patientRepository.findAll(pageable);
        } else if (caller.getRole() == Role.PROVIDER) {
            String providerId = caller.getLinkedProviderId();
            if (!StringUtils.hasText(providerId)) {
                log.warn("Provider '{}' has no linkedProviderId. Returning empty patient page.", username);
                return new PagedResponse<>(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0, 0);
            }
            page = hasSearch
                    ? patientRepository.searchByProvider(providerId, search.trim(), pageable)
                    : patientRepository.findByAssignedProviderIdsContaining(providerId, pageable);
        } else {
            // Patient role: returns only own record if matching
            if (StringUtils.hasText(caller.getLinkedPatientId())) {
                return patientRepository.findById(caller.getLinkedPatientId())
                        .map(p -> new PagedResponse<>(List.of(toSummaryDTO(p)), 0, 1, 1, 1))
                        .orElseGet(() -> new PagedResponse<>(List.of(), 0, 1, 0, 0));
            }
            return new PagedResponse<>(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0, 0);
        }

        List<PatientSummaryDTO> summaries = page.getContent().stream()
                .map(this::toSummaryDTO)
                .toList();

        return new PagedResponse<>(
                summaries,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    /**
     * Retrieves a patient record by ID.
     */
    public PatientDTO getPatientById(String patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + patientId));
        return toDTO(patient);
    }

    /**
     * Registers a new patient and automatically provisions their linked HealthTwin.
     */
    public PatientDTO createPatient(PatientDTO dto) {
        if (patientRepository.existsByMrn(dto.getMrn())) {
            throw new IllegalArgumentException("Patient with MRN '" + dto.getMrn() + "' already exists");
        }

        Patient patient = toEntity(dto);
        patient.setCreatedAt(Instant.now());
        patient.setUpdatedAt(Instant.now());

        Patient savedPatient = patientRepository.save(patient);
        log.info("Created new Patient '{}' with MRN '{}'", savedPatient.getId(), savedPatient.getMrn());

        // Automatically create associated Digital Health Twin
        healthTwinService.createInitialTwin(savedPatient);

        return toDTO(savedPatient);
    }

    /**
     * Updates an existing patient record and synchronizes changes to their HealthTwin.
     */
    public PatientDTO updatePatient(String patientId, PatientDTO dto) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + patientId));

        if (!patient.getMrn().equals(dto.getMrn()) && patientRepository.existsByMrn(dto.getMrn())) {
            throw new IllegalArgumentException("MRN '" + dto.getMrn() + "' is already in use");
        }

        patient.setFirstName(dto.getFirstName());
        patient.setLastName(dto.getLastName());
        patient.setDateOfBirth(dto.getDateOfBirth());
        patient.setGender(dto.getGender());
        patient.setEmail(dto.getEmail());
        patient.setPhone(dto.getPhone());
        patient.setAddress(toAddressEntity(dto.getAddress()));
        patient.setEmergencyContact(toEmergencyContactEntity(dto.getEmergencyContact()));
        patient.setInsuranceInfo(toInsuranceInfoEntity(dto.getInsuranceInfo()));

        if (dto.getAssignedProviderIds() != null) {
            patient.setAssignedProviderIds(dto.getAssignedProviderIds());
        }

        patient.setUpdatedAt(Instant.now());
        Patient updated = patientRepository.save(patient);

        // Sync demographic updates to HealthTwin
        twinRepository.findByPatientId(patientId).ifPresent(twin -> {
            healthTwinService.syncDemographicsFromPatient(twin, updated);
            healthTwinService.recalculateAndSave(twin, updated);
        });

        return toDTO(updated);
    }

    private PatientSummaryDTO toSummaryDTO(Patient p) {
        Double completeness = twinRepository.findByPatientId(p.getId())
                .map(t -> t.getCompleteness() != null ? t.getCompleteness().getPercentage() : 0.0)
                .orElse(0.0);

        return new PatientSummaryDTO(
                p.getId(),
                p.getMrn(),
                p.getFirstName(),
                p.getLastName(),
                p.getDateOfBirth(),
                p.getGender(),
                completeness
        );
    }

    public PatientDTO toDTO(Patient p) {
        PatientDTO dto = new PatientDTO();
        dto.setId(p.getId());
        dto.setMrn(p.getMrn());
        dto.setFirstName(p.getFirstName());
        dto.setLastName(p.getLastName());
        dto.setDateOfBirth(p.getDateOfBirth());
        dto.setGender(p.getGender());
        dto.setEmail(p.getEmail());
        dto.setPhone(p.getPhone());
        dto.setAddress(toAddressDTO(p.getAddress()));
        dto.setEmergencyContact(toEmergencyContactDTO(p.getEmergencyContact()));
        dto.setInsuranceInfo(toInsuranceInfoDTO(p.getInsuranceInfo()));
        dto.setAssignedProviderIds(p.getAssignedProviderIds());
        dto.setCreatedAt(p.getCreatedAt());
        dto.setUpdatedAt(p.getUpdatedAt());
        return dto;
    }

    public Patient toEntity(PatientDTO dto) {
        Patient p = new Patient();
        p.setId(dto.getId());
        p.setMrn(dto.getMrn());
        p.setFirstName(dto.getFirstName());
        p.setLastName(dto.getLastName());
        p.setDateOfBirth(dto.getDateOfBirth());
        p.setGender(dto.getGender());
        p.setEmail(dto.getEmail());
        p.setPhone(dto.getPhone());
        p.setAddress(toAddressEntity(dto.getAddress()));
        p.setEmergencyContact(toEmergencyContactEntity(dto.getEmergencyContact()));
        p.setInsuranceInfo(toInsuranceInfoEntity(dto.getInsuranceInfo()));
        if (dto.getAssignedProviderIds() != null) {
            p.setAssignedProviderIds(dto.getAssignedProviderIds());
        }
        return p;
    }

    private AddressDTO toAddressDTO(Address a) {
        return a != null ? new AddressDTO(a.getStreet(), a.getCity(), a.getState(), a.getZipCode(), a.getCountry()) : null;
    }

    private Address toAddressEntity(AddressDTO d) {
        return d != null ? new Address(d.getStreet(), d.getCity(), d.getState(), d.getZipCode(), d.getCountry()) : null;
    }

    private EmergencyContactDTO toEmergencyContactDTO(EmergencyContact e) {
        return e != null ? new EmergencyContactDTO(e.getName(), e.getPhone(), e.getRelationship()) : null;
    }

    private EmergencyContact toEmergencyContactEntity(EmergencyContactDTO d) {
        return d != null ? new EmergencyContact(d.getName(), d.getPhone(), d.getRelationship()) : null;
    }

    private InsuranceInfoDTO toInsuranceInfoDTO(InsuranceInfo i) {
        return i != null ? new InsuranceInfoDTO(i.getProvider(), i.getPolicyNumber()) : null;
    }

    private InsuranceInfo toInsuranceInfoEntity(InsuranceInfoDTO d) {
        return d != null ? new InsuranceInfo(d.getProvider(), d.getPolicyNumber()) : null;
    }
}
