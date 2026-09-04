package com.medisphere.auth;

import com.medisphere.auth.model.Role;
import com.medisphere.auth.model.User;
import com.medisphere.auth.repository.UserRepository;
import com.medisphere.consent.model.Consent;
import com.medisphere.consent.model.ConsentStatus;
import com.medisphere.consent.repository.ConsentRepository;
import com.medisphere.patient.model.Address;
import com.medisphere.patient.model.EmergencyContact;
import com.medisphere.patient.model.InsuranceInfo;
import com.medisphere.patient.model.Patient;
import com.medisphere.patient.repository.PatientRepository;
import com.medisphere.twin.model.HealthTwin;
import com.medisphere.twin.model.TwinDemographics;
import com.medisphere.twin.model.TwinFhirSyncStatus;
import com.medisphere.twin.model.TwinLabs;
import com.medisphere.twin.model.TwinVitals;
import com.medisphere.twin.repository.HealthTwinRepository;
import com.medisphere.twin.service.HealthTwinService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Initializes development seed data for Milestone 1:
 * <ul>
 *   <li>Demo users (Admin, Provider, Patient) with BCrypt-hashed passwords.</li>
 *   <li>Demo patients (pat-001, pat-002, pat-003) with assigned provider mappings.</li>
 *   <li>Corresponding Digital Health Twins with 100% completeness (all 20 logical fields populated).</li>
 *   <li>Active consent grant for pat-001 -> prov-001 (dr_smith).</li>
 * </ul>
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final HealthTwinRepository twinRepository;
    private final HealthTwinService healthTwinService;
    private final PasswordEncoder passwordEncoder;
    private final ConsentRepository consentRepository;

    public DataSeeder(UserRepository userRepository,
                      PatientRepository patientRepository,
                      HealthTwinRepository twinRepository,
                      HealthTwinService healthTwinService,
                      PasswordEncoder passwordEncoder,
                      ConsentRepository consentRepository) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.twinRepository = twinRepository;
        this.healthTwinService = healthTwinService;
        this.passwordEncoder = passwordEncoder;
        this.consentRepository = consentRepository;
    }

    @Override
    public void run(String... args) {
        int maxRetries = 5;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                seedUsers();
                seedPatientsAndTwins();
                seedConsents();
                break;
            } catch (Exception ex) {
                if (attempt == maxRetries) {
                    log.warn("Could not seed data after {} attempts: {}", maxRetries, ex.getMessage(), ex);
                } else {
                    log.info("MongoDB connection establishing, retrying seed in 2s (attempt {}/{})...", attempt, maxRetries);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }

    private void seedUsers() {
        if (userRepository.count() == 0) {
            log.info("Seeding demo user accounts...");

            // 1. Admin Account
            User admin = new User(
                    "admin",
                    "admin@medisphere.local",
                    passwordEncoder.encode("Admin@123"),
                    Role.ADMIN,
                    null,
                    null
            );
            userRepository.save(admin);

            // 2. Provider Account (Assigned to prov-001)
            User provider = new User(
                    "dr_smith",
                    "dr.smith@hospital.org",
                    passwordEncoder.encode("Provider@123"),
                    Role.PROVIDER,
                    null,
                    "prov-001"
            );
            userRepository.save(provider);

            // 3. Patient Account (Linked to pat-001)
            User patient = new User(
                    "john_doe",
                    "john.doe@patient.org",
                    passwordEncoder.encode("Patient@123"),
                    Role.PATIENT,
                    "pat-001",
                    null
            );
            userRepository.save(patient);

            log.info("Seeded 3 demo user accounts: 'admin', 'dr_smith', 'john_doe'.");
        }
    }

    private void seedPatientsAndTwins() {
        if (patientRepository.count() == 0) {
            log.info("Seeding demo patients and corresponding 100% complete HealthTwins...");

            // --- Patient 1: John Doe (Assigned to prov-001 / dr_smith) ---
            Patient pat1 = new Patient("MRN-10001", "John", "Doe", LocalDate.of(1980, 5, 15), "Male");
            pat1.setId("pat-001");
            pat1.setEmail("john.doe@patient.org");
            pat1.setPhone("+1-555-0101");
            pat1.setAddress(new Address("124 Healthcare Way", "Boston", "MA", "02115", "USA"));
            pat1.setEmergencyContact(new EmergencyContact("Mary Doe", "+1-555-0102", "Spouse"));
            pat1.setInsuranceInfo(new InsuranceInfo("BlueCross", "BC-987654321"));
            pat1.setAssignedProviderIds(List.of("prov-001"));
            patientRepository.save(pat1);
            createCompleteTwin(pat1, 178.0, 78.0, "O+", 72.0, 120.0, 80.0, 98.0, 36.6, 16.0, 95.0, 185.0, 14.8, 0.9);

            // --- Patient 2: Jane Roe (Assigned to prov-001 / dr_smith) ---
            Patient pat2 = new Patient("MRN-10002", "Jane", "Roe", LocalDate.of(1992, 8, 22), "Female");
            pat2.setId("pat-002");
            pat2.setEmail("jane.roe@patient.org");
            pat2.setPhone("+1-555-0201");
            pat2.setAddress(new Address("45 Beacon Street", "Boston", "MA", "02108", "USA"));
            pat2.setEmergencyContact(new EmergencyContact("David Roe", "+1-555-0202", "Brother"));
            pat2.setInsuranceInfo(new InsuranceInfo("Aetna", "AET-44332211"));
            pat2.setAssignedProviderIds(List.of("prov-001"));
            patientRepository.save(pat2);
            createCompleteTwin(pat2, 165.0, 62.0, "A+", 68.0, 115.0, 75.0, 99.0, 36.8, 14.0, 88.0, 170.0, 13.5, 0.8);

            // --- Patient 3: Robert Chen (Assigned to prov-002, NOT assigned to prov-001 / dr_smith) ---
            Patient pat3 = new Patient("MRN-10003", "Robert", "Chen", LocalDate.of(1975, 11, 3), "Male");
            pat3.setId("pat-003");
            pat3.setEmail("robert.chen@patient.org");
            pat3.setPhone("+1-555-0301");
            pat3.setAddress(new Address("78 Cambridge Center", "Cambridge", "MA", "02142", "USA"));
            pat3.setEmergencyContact(new EmergencyContact("Lisa Chen", "+1-555-0302", "Spouse"));
            pat3.setInsuranceInfo(new InsuranceInfo("UnitedHealth", "UH-55667788"));
            pat3.setAssignedProviderIds(List.of("prov-002")); // Unassigned to dr_smith!
            patientRepository.save(pat3);
            createCompleteTwin(pat3, 172.0, 70.0, "B+", 76.0, 128.0, 84.0, 97.0, 36.5, 18.0, 102.0, 195.0, 15.1, 1.0);

            log.info("==================================================================");
            log.info("Demo Patients & 100% Complete HealthTwins Seeded:");
            log.info("  - pat-001: John Doe    (MRN-10001, Assigned to prov-001 [dr_smith]) -> Completeness: 100%");
            log.info("  - pat-002: Jane Roe    (MRN-10002, Assigned to prov-001 [dr_smith]) -> Completeness: 100%");
            log.info("  - pat-003: Robert Chen (MRN-10003, Assigned to prov-002 [Dr. Lee])  -> Completeness: 100%");
            log.info("==================================================================");
        }
    }

    private void createCompleteTwin(Patient patient, double height, double weight, String bloodType,
                                    double hr, double sbp, double dbp, double spo2, double temp, double rr,
                                    double glucose, double chol, double hb, double creat) {
        HealthTwin twin = new HealthTwin(patient.getId());

        // Demographics (height, weight, bloodType, age, gender, bmi)
        healthTwinService.syncDemographicsFromPatient(twin, patient);
        twin.getDemographics().setHeight(height);
        twin.getDemographics().setWeight(weight);
        twin.getDemographics().setBloodType(bloodType);
        healthTwinService.syncDemographicsFromPatient(twin, patient); // recalculates bmi

        // Latest Vitals (heartRate, systolicBP, diastolicBP, oxygenSaturation, temperature, respiratoryRate)
        twin.setLatestVitals(new TwinVitals(hr, sbp, dbp, spo2, temp, rr, Instant.now()));

        // Latest Labs (glucose, cholesterol, hemoglobin, creatinine)
        twin.setLatestLabs(new TwinLabs(glucose, chol, hb, creat, Instant.now()));

        // FHIR Sync Status (syncStatus)
        twin.setFhirSyncStatus(new TwinFhirSyncStatus(Instant.now(), 10, "SYNCED"));

        // Calculate and verify 100% completeness
        healthTwinService.recalculateAndSave(twin, patient);
    }

    private void seedConsents() {
        if (consentRepository.count() == 0) {
            log.info("Seeding demo patient consents...");

            Consent consent1 = new Consent(
                    "pat-001",
                    "prov-001",
                    "treatment",
                    ConsentStatus.GRANTED,
                    Instant.now(),
                    Instant.now().plus(365, ChronoUnit.DAYS),
                    "Primary care and cognitive twin data sharing authorization"
            );
            consentRepository.save(consent1);

            log.info("==================================================================");
            log.info("Demo Consents Seeded:");
            log.info("  - pat-001 (John Doe) -> prov-001 (dr_smith): GRANTED (1-year expiration)");
            log.info("  - pat-002 (Jane Roe): NO active consent (demonstrates access denial)");
            log.info("==================================================================");
        }
    }
}
