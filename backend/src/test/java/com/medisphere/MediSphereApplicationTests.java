package com.medisphere;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies the Spring Boot application context loads successfully.
 *
 * <p>Uses 'dev' profile. Requires MongoDB and Kafka to be running
 * (via Docker Compose) for a full context load. If infrastructure
 * is unavailable, individual unit tests should still pass.</p>
 */
@SpringBootTest
@ActiveProfiles("dev")
class MediSphereApplicationTests {

    @Test
    void contextLoads() {
        // Verifies application context starts without errors
    }
}
