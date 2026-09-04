package com.medisphere.patient;

import com.medisphere.auth.model.Role;
import com.medisphere.auth.model.User;
import com.medisphere.auth.repository.UserRepository;
import com.medisphere.common.dto.PagedResponse;
import com.medisphere.patient.dto.PatientDTO;
import com.medisphere.patient.dto.PatientSummaryDTO;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private HealthTwinRepository twinRepository;

    @Mock
    private HealthTwinService healthTwinService;

    @Mock
    private UserRepository userRepository;

    private PatientService patientService;

    @BeforeEach
    void setUp() {
        patientService = new PatientService(patientRepository, twinRepository, healthTwinService, userRepository);
    }

    @Test
    @DisplayName("Creating a patient automatically triggers initial HealthTwin creation")
    void testCreatePatientTriggersTwinCreation() {
        PatientDTO dto = new PatientDTO();
        dto.setMrn("MRN-99999");
        dto.setFirstName("Alice");
        dto.setLastName("Wonder");
        dto.setDateOfBirth(LocalDate.of(1995, 6, 10));
        dto.setGender("Female");
        dto.setAssignedProviderIds(List.of("prov-001"));

        when(patientRepository.existsByMrn("MRN-99999")).thenReturn(false);
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> {
            Patient p = invocation.getArgument(0);
            p.setId("pat-new-123");
            return p;
        });

        PatientDTO created = patientService.createPatient(dto);

        assertNotNull(created);
        assertEquals("pat-new-123", created.getId());
        assertEquals("MRN-99999", created.getMrn());

        // Verify that HealthTwinService.createInitialTwin was invoked with the saved patient
        ArgumentCaptor<Patient> patientCaptor = ArgumentCaptor.forClass(Patient.class);
        verify(healthTwinService).createInitialTwin(patientCaptor.capture());
        assertEquals("pat-new-123", patientCaptor.getValue().getId());
    }

    @Test
    @DisplayName("Creating patient with duplicate MRN throws IllegalArgumentException")
    void testDuplicateMrnThrowsException() {
        PatientDTO dto = new PatientDTO();
        dto.setMrn("MRN-EXISTING");

        when(patientRepository.existsByMrn("MRN-EXISTING")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> patientService.createPatient(dto));
        verify(patientRepository, never()).save(any());
        verify(healthTwinService, never()).createInitialTwin(any());
    }

    @Test
    @DisplayName("Provider retrieves only assigned patients")
    void testProviderRetrievesOnlyAssignedPatients() {
        User providerUser = new User("dr_smith", "dr.smith@hospital.org", "hash", Role.PROVIDER, null, "prov-001");
        when(userRepository.findByUsername("dr_smith")).thenReturn(Optional.of(providerUser));

        Patient assignedPatient = new Patient("MRN-1", "John", "Doe", LocalDate.of(1980, 1, 1), "Male");
        assignedPatient.setId("pat-001");
        assignedPatient.setAssignedProviderIds(List.of("prov-001"));

        Pageable pageable = PageRequest.of(0, 20);
        when(patientRepository.findByAssignedProviderIdsContaining(eq("prov-001"), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(assignedPatient), pageable, 1));

        PagedResponse<PatientSummaryDTO> response = patientService.getPatients("dr_smith", null, pageable);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals("pat-001", response.getContent().get(0).getId());
        verify(patientRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Admin retrieves all patients across all providers")
    void testAdminRetrievesAllPatients() {
        User adminUser = new User("admin", "admin@medisphere.local", "hash", Role.ADMIN, null, null);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        Patient p1 = new Patient("MRN-1", "John", "Doe", LocalDate.of(1980, 1, 1), "Male");
        p1.setId("pat-001");
        Patient p2 = new Patient("MRN-2", "Jane", "Roe", LocalDate.of(1990, 2, 2), "Female");
        p2.setId("pat-002");

        Pageable pageable = PageRequest.of(0, 20);
        when(patientRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(p1, p2), pageable, 2));

        PagedResponse<PatientSummaryDTO> response = patientService.getPatients("admin", null, pageable);

        assertNotNull(response);
        assertEquals(2, response.getContent().size());
        verify(patientRepository).findAll(pageable);
    }
}
