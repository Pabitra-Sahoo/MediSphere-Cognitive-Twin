package com.medisphere.auth;

import com.medisphere.auth.model.Role;
import com.medisphere.auth.model.User;
import com.medisphere.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Initializes development seed users if the user collection is empty.
 *
 * <p>Demo accounts provided:
 * <ul>
 *   <li>ADMIN: {@code admin} / {@code Admin@123}</li>
 *   <li>PROVIDER: {@code dr_smith} / {@code Provider@123} (linkedProviderId: prov-001)</li>
 *   <li>PATIENT: {@code john_doe} / {@code Patient@123} (linkedPatientId: pat-001)</li>
 * </ul>
 * Passwords are never stored in plaintext and are hashed using BCrypt.</p>
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        try {
            if (userRepository.count() == 0) {
                log.info("No users found in database. Seeding demo accounts for Milestone 1...");

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

                // 2. Provider Account
                User provider = new User(
                        "dr_smith",
                        "dr.smith@hospital.org",
                        passwordEncoder.encode("Provider@123"),
                        Role.PROVIDER,
                        null,
                        "prov-001"
                );
                userRepository.save(provider);

                // 3. Patient Account
                User patient = new User(
                        "john_doe",
                        "john.doe@patient.org",
                        passwordEncoder.encode("Patient@123"),
                        Role.PATIENT,
                        "pat-001",
                        null
                );
                userRepository.save(patient);

                log.info("==================================================================");
                log.info("MediSphere Demo Accounts Seeded (BCrypt Hashed):");
                log.info("  - ADMIN:    username='admin'     (Role: ADMIN)");
                log.info("  - PROVIDER: username='dr_smith'  (Role: PROVIDER, providerId: prov-001)");
                log.info("  - PATIENT:  username='john_doe'  (Role: PATIENT,  patientId: pat-001)");
                log.info("==================================================================");
            } else {
                log.info("Database already contains {} user(s). Skipping seed data.", userRepository.count());
            }
        } catch (Exception ex) {
            log.warn("Could not seed users (MongoDB might be starting or unavailable): {}", ex.getMessage());
        }
    }
}
