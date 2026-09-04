package com.medisphere.fhir;

import com.medisphere.fhir.mapper.FhirToTwinMapper;
import com.medisphere.lab.model.LabResult;
import com.medisphere.patient.model.Patient;
import com.medisphere.twin.model.HealthTwin;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("FhirToTwinMapper Tests")
class FhirToTwinMapperTest {

    private FhirToTwinMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new FhirToTwinMapper();
    }

    @Test
    @DisplayName("Maps FHIR Patient demographics to domain Patient entity")
    void testMapPatientDemographics() {
        org.hl7.fhir.r4.model.Patient fhirPatient = new org.hl7.fhir.r4.model.Patient();
        fhirPatient.addName(new HumanName().setFamily("Roe").addGiven("Jane"));
        fhirPatient.setGender(org.hl7.fhir.r4.model.Enumerations.AdministrativeGender.FEMALE);
        fhirPatient.setBirthDate(new Date(88, 6, 14)); // 1988-07-14
        fhirPatient.addAddress(new Address().addLine("456 Elm Ave").setCity("Metropolis").setState("NY").setPostalCode("10002"));

        Patient domainPatient = new Patient();
        mapper.mapPatientDemographics(fhirPatient, domainPatient);

        assertEquals("Jane", domainPatient.getFirstName());
        assertEquals("Roe", domainPatient.getLastName());
        assertEquals("Female", domainPatient.getGender());
        assertEquals(LocalDate.of(1988, 7, 14), domainPatient.getDateOfBirth());
        assertNotNull(domainPatient.getAddress());
        assertEquals("456 Elm Ave", domainPatient.getAddress().getStreet());
        assertEquals("Metropolis", domainPatient.getAddress().getCity());
        assertEquals("10002", domainPatient.getAddress().getZipCode());
    }

    @Test
    @DisplayName("Maps vital observations (HR, BP panel, SpO2, Temp, RR) to HealthTwin")
    void testMapVitalObservations() {
        HealthTwin twin = new HealthTwin("pat-001");

        // 1. Heart Rate
        Observation hrObs = new Observation();
        hrObs.setStatus(Observation.ObservationStatus.FINAL);
        hrObs.setCode(new CodeableConcept().addCoding(new Coding("http://loinc.org", "8867-4", "Heart rate")));
        hrObs.setValue(new Quantity().setValue(72).setUnit("/min"));
        hrObs.setEffective(new DateTimeType("2026-09-04T10:00:00Z"));
        boolean hrMapped = mapper.mapVitalObservation(hrObs, twin);
        assertTrue(hrMapped);
        assertEquals(72.0, twin.getLatestVitals().getHeartRate());

        // 2. Blood Pressure Panel
        Observation bpObs = new Observation();
        bpObs.setStatus(Observation.ObservationStatus.FINAL);
        bpObs.setCode(new CodeableConcept().addCoding(new Coding("http://loinc.org", "85354-9", "Blood pressure")));
        bpObs.addComponent(new Observation.ObservationComponentComponent()
                .setCode(new CodeableConcept().addCoding(new Coding("http://loinc.org", "8480-6", "Systolic")))
                .setValue(new Quantity().setValue(120).setUnit("mmHg")));
        bpObs.addComponent(new Observation.ObservationComponentComponent()
                .setCode(new CodeableConcept().addCoding(new Coding("http://loinc.org", "8462-4", "Diastolic")))
                .setValue(new Quantity().setValue(80).setUnit("mmHg")));
        boolean bpMapped = mapper.mapVitalObservation(bpObs, twin);
        assertTrue(bpMapped);
        assertEquals(120.0, twin.getLatestVitals().getSystolicBP());
        assertEquals(80.0, twin.getLatestVitals().getDiastolicBP());

        // 3. SpO2
        Observation spo2Obs = new Observation();
        spo2Obs.setStatus(Observation.ObservationStatus.FINAL);
        spo2Obs.setCode(new CodeableConcept().addCoding(new Coding("http://loinc.org", "2708-6", "Oxygen saturation")));
        spo2Obs.setValue(new Quantity().setValue(98.0).setUnit("%"));
        assertTrue(mapper.mapVitalObservation(spo2Obs, twin));
        assertEquals(98.0, twin.getLatestVitals().getOxygenSaturation());
    }

    @Test
    @DisplayName("Maps height, weight, and calculates BMI")
    void testHeightWeightAndBmi() {
        HealthTwin twin = new HealthTwin("pat-001");

        Observation heightObs = new Observation();
        heightObs.setStatus(Observation.ObservationStatus.FINAL);
        heightObs.setCode(new CodeableConcept().addCoding(new Coding("http://loinc.org", "8302-2", "Body height")));
        heightObs.setValue(new Quantity().setValue(180.0).setUnit("cm"));
        assertTrue(mapper.mapVitalObservation(heightObs, twin));
        assertEquals(180.0, twin.getDemographics().getHeight());

        Observation weightObs = new Observation();
        weightObs.setStatus(Observation.ObservationStatus.FINAL);
        weightObs.setCode(new CodeableConcept().addCoding(new Coding("http://loinc.org", "29463-7", "Body weight")));
        weightObs.setValue(new Quantity().setValue(81.0).setUnit("kg"));
        assertTrue(mapper.mapVitalObservation(weightObs, twin));
        assertEquals(81.0, twin.getDemographics().getWeight());

        // 81 / (1.8 * 1.8) = 81 / 3.24 = 25.0
        assertEquals(25.0, twin.getDemographics().getBmi());
    }

    @Test
    @DisplayName("Maps lab observations to HealthTwin and generates LabResult entity")
    void testMapLabObservation() {
        HealthTwin twin = new HealthTwin("pat-001");

        Observation gluObs = new Observation();
        gluObs.setStatus(Observation.ObservationStatus.FINAL);
        gluObs.setCode(new CodeableConcept().addCoding(new Coding("http://loinc.org", "2345-7", "Glucose")));
        gluObs.setValue(new Quantity().setValue(98.0).setUnit("mg/dL"));
        gluObs.addReferenceRange(new Observation.ObservationReferenceRangeComponent()
                .setLow(new Quantity().setValue(70.0))
                .setHigh(new Quantity().setValue(99.0)));

        Optional<LabResult> labResultOpt = mapper.mapLabObservation(gluObs, twin, "pat-001");
        assertTrue(labResultOpt.isPresent());
        LabResult lab = labResultOpt.get();

        assertEquals(98.0, twin.getLatestLabs().getGlucose());
        assertEquals("pat-001", lab.getPatientId());
        assertEquals("Glucose", lab.getTestName());
        assertEquals(98.0, lab.getValue());
        assertEquals("mg/dL", lab.getUnit());
        assertEquals("FINAL", lab.getStatus());
        assertNotNull(lab.getReferenceRange());
        assertEquals(70.0, lab.getReferenceRange().getLow());
        assertEquals(99.0, lab.getReferenceRange().getHigh());
    }

    @Test
    @DisplayName("Extracts MRN and subject references correctly")
    void testExtractIdentifiers() {
        org.hl7.fhir.r4.model.Patient patient = new org.hl7.fhir.r4.model.Patient();
        patient.addIdentifier(new Identifier().setSystem("http://medisphere.com/mrn").setValue("MRN-10001"));
        Optional<String> mrn = mapper.extractMrn(patient);
        assertTrue(mrn.isPresent());
        assertEquals("MRN-10001", mrn.get());

        Observation obs = new Observation();
        obs.setSubject(new Reference("Patient/pat-001"));
        Optional<String> subj = mapper.extractSubjectId(obs);
        assertTrue(subj.isPresent());
        assertEquals("pat-001", subj.get());
    }
}
