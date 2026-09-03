package com.medisphere;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MediSphere Cognitive Twin — Spring Boot Application Entry Point.
 *
 * <p>AI-based healthcare management platform providing FHIR R4 integration,
 * Digital Health Twin management, Kafka vitals streaming, and Patient 360 APIs.</p>
 */
@SpringBootApplication
public class MediSphereApplication {

    public static void main(String[] args) {
        SpringApplication.run(MediSphereApplication.class, args);
    }
}
