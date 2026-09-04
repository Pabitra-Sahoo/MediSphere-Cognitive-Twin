package com.medisphere.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration exposing HAPI FHIR R4 context and validator.
 */
@Configuration
public class FhirConfig {

    @Bean
    public FhirContext fhirContext() {
        return FhirContext.forR4Cached();
    }

    @Bean
    public FhirValidator fhirValidator(FhirContext fhirContext) {
        FhirValidator validator = fhirContext.newValidator();
        validator.setValidateAgainstStandardSchema(true);
        validator.setValidateAgainstStandardSchematron(false);
        return validator;
    }
}
