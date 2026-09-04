package com.medisphere.twin;

import com.medisphere.patient.model.Address;
import com.medisphere.patient.model.EmergencyContact;
import com.medisphere.patient.model.InsuranceInfo;
import com.medisphere.patient.model.Patient;
import com.medisphere.patient.repository.PatientRepository;
import com.medisphere.twin.model.HealthTwin;
import com.medisphere.twin.model.TwinCompleteness;
import com.medisphere.twin.model.TwinDemographics;
import com.medisphere.twin.model.TwinFhirSyncStatus;
import com.medisphere.twin.model.TwinLabs;
import com.medisphere.twin.model.TwinVitals;
import com.medisphere.twin.repository.HealthTwinRepository;
import com.medisphere.twin.service.HealthTwinService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class HealthTwinCompletenessTest {

    @Mock
    private HealthTwinRepository twinRepository;

    @Mock
    private PatientRepository patientRepository;

    private HealthTwinService twinService;

    @BeforeEach
    void setUp() {
        twinService = new HealthTwinService(twinRepository, patientRepository);
    }

    private Patient createCompletePatient() {
        Patient p = new Patient("MRN-10001", "John", "Doe", LocalDate.of(1985, 3, 20), "Male");
        p.setId("pat-001");
        p.setEmail("john.doe@test.org");
        p.setPhone("+1-555-0101");
        p.setAddress(new Address("123 Health St", "Boston", "MA", "02115", "USA"));
        p.setEmergencyContact(new EmergencyContact("Mary Doe", "+1-555-0102", "Spouse"));
        p.setInsuranceInfo(new InsuranceInfo("BlueCross", "BC-12345"));
        p.setAssignedProviderIds(List.of("prov-001"));
        return p;
    }

    private HealthTwin createCompleteTwin(String patientId) {
        HealthTwin twin = new HealthTwin(patientId);
        twin.setDemographics(new TwinDemographics(39, "Male", 24.5, 178.0, 78.0, "O+"));
        twin.setLatestVitals(new TwinVitals(72.0, 120.0, 80.0, 98.0, 36.6, 16.0, Instant.now()));
        twin.setLatestLabs(new TwinLabs(95.0, 180.0, 14.5, 0.9, Instant.now()));
        twin.setFhirSyncStatus(new TwinFhirSyncStatus(Instant.now(), 5, "SYNCED"));
        return twin;
    }

    @Test
    @DisplayName("100% Completeness: All 20 logical fields populated passes >95% requirement")
    void testHundredPercentCompleteness() {
        Patient patient = createCompletePatient();
        HealthTwin twin = createCompleteTwin(patient.getId());

        TwinCompleteness completeness = twinService.calculateCompleteness(twin, patient);

        assertEquals(20, completeness.getTotalFields());
        assertEquals(20, completeness.getPopulatedFields());
        assertEquals(100.0, completeness.getPercentage(), 0.001);
        assertTrue(completeness.getMissingFields().isEmpty(), "No missing fields should be reported");

        // Explicitly assert that 100% satisfies the strict M1 >95% acceptance criteria
        assertTrue(completeness.getPercentage() > 95.0, "100% completeness must strictly exceed 95%");
    }

    @Test
    @DisplayName("Boundary Test (95.0%): 19 of 20 fields populated does NOT pass strictly >95% requirement")
    void testNinetyFivePercentBoundaryFailsStrictRequirement() {
        Patient patient = createCompletePatient();
        HealthTwin twin = createCompleteTwin(patient.getId());

        // Omit exactly 1 logical field: bloodType
        twin.getDemographics().setBloodType(null);

        TwinCompleteness completeness = twinService.calculateCompleteness(twin, patient);

        assertEquals(20, completeness.getTotalFields());
        assertEquals(19, completeness.getPopulatedFields());
        assertEquals(95.0, completeness.getPercentage(), 0.001);
        assertEquals(1, completeness.getMissingFields().size());
        assertTrue(completeness.getMissingFields().contains("bloodType"));

        // Crucial requirement assertion: 95.0% is NOT strictly greater than 95.0%!
        assertFalse(completeness.getPercentage() > 95.0,
                "Exactly 95.0% (19/20) must NOT pass the strictly >95% M1 acceptance criteria");
    }

    @Test
    @DisplayName("Below 95% Completeness: 18 of 20 fields populated gives 90.0%")
    void testEighteenFieldsPopulatedGivesNinetyPercent() {
        Patient patient = createCompletePatient();
        HealthTwin twin = createCompleteTwin(patient.getId());

        // Omit 2 fields: glucose and cholesterol
        twin.getLatestLabs().setGlucose(null);
        twin.getLatestLabs().setCholesterol(null);

        TwinCompleteness completeness = twinService.calculateCompleteness(twin, patient);

        assertEquals(20, completeness.getTotalFields());
        assertEquals(18, completeness.getPopulatedFields());
        assertEquals(90.0, completeness.getPercentage(), 0.001);
        assertEquals(2, completeness.getMissingFields().size());
        assertTrue(completeness.getMissingFields().contains("glucose"));
        assertTrue(completeness.getMissingFields().contains("cholesterol"));
        assertFalse(completeness.getPercentage() > 95.0);
    }

    @Test
    @DisplayName("Missing logical nested objects are accurately reported as missing")
    void testMissingNestedObjects() {
        Patient patient = createCompletePatient();
        HealthTwin twin = createCompleteTwin(patient.getId());

        // Omit emergency contact phone
        patient.getEmergencyContact().setPhone("");

        // Omit fhirSyncStatus syncStatus
        twin.getFhirSyncStatus().setSyncStatus(null);

        TwinCompleteness completeness = twinService.calculateCompleteness(twin, patient);

        assertEquals(18, completeness.getPopulatedFields());
        assertEquals(90.0, completeness.getPercentage(), 0.001);
        assertTrue(completeness.getMissingFields().contains("emergencyContact"));
        assertTrue(completeness.getMissingFields().contains("fhirSyncStatus"));
    }

    @Test
    @DisplayName("Empty twin and patient starts with 0 populated fields")
    void testEmptyTwinAndPatient() {
        Patient patient = new Patient();
        HealthTwin twin = new HealthTwin();

        TwinCompleteness completeness = twinService.calculateCompleteness(twin, patient);

        assertEquals(20, completeness.getTotalFields());
        assertEquals(0, completeness.getPopulatedFields());
        assertEquals(0.0, completeness.getPercentage(), 0.001);
        assertEquals(20, completeness.getMissingFields().size());
    }
}
