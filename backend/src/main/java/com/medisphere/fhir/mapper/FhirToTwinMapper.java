package com.medisphere.fhir.mapper;

import com.medisphere.lab.model.LabResult;
import com.medisphere.patient.model.Address;
import com.medisphere.patient.model.EmergencyContact;
import com.medisphere.patient.model.Patient;
import com.medisphere.twin.model.HealthTwin;
import com.medisphere.twin.model.TwinDemographics;
import com.medisphere.twin.model.TwinLabs;
import com.medisphere.twin.model.TwinVitals;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Consent;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Component responsible for transforming FHIR R4 resources into
 * Patient, HealthTwin, and LabResult domain models.
 */
@Component
public class FhirToTwinMapper {

    // Common LOINC Codes
    public static final String LOINC_HEART_RATE = "8867-4";
    public static final String LOINC_BP_PANEL = "85354-9";
    public static final String LOINC_BP_SYSTOLIC = "8480-6";
    public static final String LOINC_BP_DIASTOLIC = "8462-4";
    public static final String LOINC_OXYGEN_SAT = "2708-6";
    public static final String LOINC_OXYGEN_SAT_ALT = "59408-5";
    public static final String LOINC_BODY_TEMP = "8310-5";
    public static final String LOINC_RESP_RATE = "9279-1";
    public static final String LOINC_BODY_HEIGHT = "8302-2";
    public static final String LOINC_BODY_WEIGHT = "29463-7";
    public static final String LOINC_BODY_WEIGHT_ALT = "3141-9";

    // Lab LOINC Codes
    public static final String LOINC_GLUCOSE = "2345-7";
    public static final String LOINC_GLUCOSE_ALT = "2339-0";
    public static final String LOINC_CHOLESTEROL = "2093-3";
    public static final String LOINC_HEMOGLOBIN = "718-7";
    public static final String LOINC_CREATININE = "2160-0";

    /**
     * Extracts MRN or primary identifier from a FHIR Patient resource.
     */
    public Optional<String> extractMrn(org.hl7.fhir.r4.model.Patient fhirPatient) {
        if (fhirPatient == null || !fhirPatient.hasIdentifier()) {
            return Optional.empty();
        }

        // 1. Prefer MRN-specific identifier
        for (Identifier id : fhirPatient.getIdentifier()) {
            if (id.hasType() && id.getType().hasCoding()) {
                for (Coding c : id.getType().getCoding()) {
                    if ("MR".equalsIgnoreCase(c.getCode())) {
                        return Optional.ofNullable(id.getValue());
                    }
                }
            }
            if (id.hasSystem() && id.getSystem().toLowerCase().contains("mrn")) {
                return Optional.ofNullable(id.getValue());
            }
        }

        // 2. Fallback to first non-blank identifier
        for (Identifier id : fhirPatient.getIdentifier()) {
            if (StringUtils.hasText(id.getValue())) {
                return Optional.of(id.getValue());
            }
        }

        return Optional.empty();
    }

    /**
     * Extracts referenced patient ID from a FHIR resource subject/patient field.
     */
    public Optional<String> extractSubjectId(IBaseResource resource) {
        if (resource == null) {
            return Optional.empty();
        }

        Reference ref = null;
        if (resource instanceof Observation obs && obs.hasSubject()) {
            ref = obs.getSubject();
        } else if (resource instanceof DiagnosticReport report && report.hasSubject()) {
            ref = report.getSubject();
        } else if (resource instanceof Consent consent && consent.hasPatient()) {
            ref = consent.getPatient();
        }

        if (ref != null && ref.hasReference()) {
            String refStr = ref.getReference();
            if (refStr.startsWith("Patient/")) {
                return Optional.of(refStr.substring("Patient/".length()).trim());
            }
            return Optional.of(refStr.trim());
        }

        return Optional.empty();
    }

    /**
     * Maps FHIR Patient demographics into domain Patient entity.
     */
    public void mapPatientDemographics(org.hl7.fhir.r4.model.Patient fhirPatient, Patient domainPatient) {
        if (fhirPatient == null || domainPatient == null) {
            return;
        }

        // Name
        if (fhirPatient.hasName()) {
            HumanName name = fhirPatient.getNameFirstRep();
            if (name.hasGiven()) {
                domainPatient.setFirstName(String.join(" ", name.getGivenAsSingleString()));
            }
            if (name.hasFamily()) {
                domainPatient.setLastName(name.getFamily());
            }
        }

        // BirthDate
        if (fhirPatient.hasBirthDate()) {
            Date bd = fhirPatient.getBirthDate();
            domainPatient.setDateOfBirth(bd.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        }

        // Gender
        if (fhirPatient.hasGender()) {
            String code = fhirPatient.getGender().toCode();
            domainPatient.setGender(code.substring(0, 1).toUpperCase() + code.substring(1).toLowerCase());
        }

        // Telecom
        if (fhirPatient.hasTelecom()) {
            for (ContactPoint cp : fhirPatient.getTelecom()) {
                if (cp.getSystem() == ContactPoint.ContactPointSystem.PHONE && !StringUtils.hasText(domainPatient.getPhone())) {
                    domainPatient.setPhone(cp.getValue());
                } else if (cp.getSystem() == ContactPoint.ContactPointSystem.EMAIL && !StringUtils.hasText(domainPatient.getEmail())) {
                    domainPatient.setEmail(cp.getValue());
                }
            }
        }

        // Address
        if (fhirPatient.hasAddress()) {
            org.hl7.fhir.r4.model.Address fhirAddr = fhirPatient.getAddressFirstRep();
            Address addr = domainPatient.getAddress() != null ? domainPatient.getAddress() : new Address();
            if (fhirAddr.hasLine()) {
                addr.setStreet(fhirAddr.getLine().get(0).getValue());
            }
            if (fhirAddr.hasCity()) {
                addr.setCity(fhirAddr.getCity());
            }
            if (fhirAddr.hasState()) {
                addr.setState(fhirAddr.getState());
            }
            if (fhirAddr.hasPostalCode()) {
                addr.setZipCode(fhirAddr.getPostalCode());
            }
            if (fhirAddr.hasCountry()) {
                addr.setCountry(fhirAddr.getCountry());
            }
            domainPatient.setAddress(addr);
        }

        // Emergency Contact
        if (fhirPatient.hasContact()) {
            org.hl7.fhir.r4.model.Patient.ContactComponent contact = fhirPatient.getContactFirstRep();
            EmergencyContact ec = domainPatient.getEmergencyContact() != null ? domainPatient.getEmergencyContact() : new EmergencyContact();
            if (contact.hasName()) {
                ec.setName(contact.getName().getNameAsSingleString());
            }
            if (contact.hasRelationship() && contact.getRelationshipFirstRep().hasText()) {
                ec.setRelationship(contact.getRelationshipFirstRep().getText());
            }
            if (contact.hasTelecom()) {
                ec.setPhone(contact.getTelecomFirstRep().getValue());
            }
            domainPatient.setEmergencyContact(ec);
        }

        domainPatient.setUpdatedAt(Instant.now());
    }

    /**
     * Synchronizes patient demographics to the HealthTwin document.
     */
    public void syncDemographicsToTwin(Patient patient, HealthTwin twin) {
        if (patient == null || twin == null) {
            return;
        }

        TwinDemographics demo = twin.getDemographics() != null ? twin.getDemographics() : new TwinDemographics();
        demo.setGender(patient.getGender());

        if (patient.getDateOfBirth() != null) {
            demo.setAge(Period.between(patient.getDateOfBirth(), LocalDate.now()).getYears());
        }

        twin.setDemographics(demo);
        twin.setUpdatedAt(Instant.now());
    }

    /**
     * Maps a vital-signs Observation to the HealthTwin.
     *
     * @return true if observation was mapped to vitals or demographics
     */
    public boolean mapVitalObservation(Observation obs, HealthTwin twin) {
        if (obs == null || twin == null || !obs.hasCode()) {
            return false;
        }

        TwinVitals vitals = twin.getLatestVitals() != null ? twin.getLatestVitals() : new TwinVitals();
        TwinDemographics demo = twin.getDemographics() != null ? twin.getDemographics() : new TwinDemographics();

        Instant timestamp = extractObservationInstant(obs);
        if (timestamp != null) {
            vitals.setTimestamp(timestamp);
        }

        boolean mapped = false;

        // 1. Blood pressure panel (LOINC 85354-9) with components
        if (hasLoincCode(obs, LOINC_BP_PANEL) && obs.hasComponent()) {
            for (Observation.ObservationComponentComponent comp : obs.getComponent()) {
                if (comp.hasValueQuantity()) {
                    double val = comp.getValueQuantity().getValue().doubleValue();
                    if (hasLoincCode(comp.getCode(), LOINC_BP_SYSTOLIC)) {
                        vitals.setSystolicBP(val);
                        mapped = true;
                    } else if (hasLoincCode(comp.getCode(), LOINC_BP_DIASTOLIC)) {
                        vitals.setDiastolicBP(val);
                        mapped = true;
                    }
                }
            }
        }

        // 2. Direct single-quantity vital observations
        if (obs.hasValueQuantity()) {
            double val = obs.getValueQuantity().getValue().doubleValue();

            if (hasLoincCode(obs, LOINC_HEART_RATE) || matchesText(obs, "heart rate")) {
                vitals.setHeartRate(val);
                mapped = true;
            } else if (hasLoincCode(obs, LOINC_BP_SYSTOLIC) || matchesText(obs, "systolic")) {
                vitals.setSystolicBP(val);
                mapped = true;
            } else if (hasLoincCode(obs, LOINC_BP_DIASTOLIC) || matchesText(obs, "diastolic")) {
                vitals.setDiastolicBP(val);
                mapped = true;
            } else if (hasLoincCode(obs, LOINC_OXYGEN_SAT, LOINC_OXYGEN_SAT_ALT) || matchesText(obs, "oxygen", "spo2")) {
                vitals.setOxygenSaturation(val);
                mapped = true;
            } else if (hasLoincCode(obs, LOINC_BODY_TEMP) || matchesText(obs, "temperature")) {
                vitals.setTemperature(val);
                mapped = true;
            } else if (hasLoincCode(obs, LOINC_RESP_RATE) || matchesText(obs, "respiratory")) {
                vitals.setRespiratoryRate(val);
                mapped = true;
            } else if (hasLoincCode(obs, LOINC_BODY_HEIGHT) || matchesText(obs, "height")) {
                demo.setHeight(val);
                recalculateBmi(demo);
                mapped = true;
            } else if (hasLoincCode(obs, LOINC_BODY_WEIGHT, LOINC_BODY_WEIGHT_ALT) || matchesText(obs, "weight")) {
                demo.setWeight(val);
                recalculateBmi(demo);
                mapped = true;
            }
        }

        if (mapped) {
            twin.setLatestVitals(vitals);
            twin.setDemographics(demo);
            twin.setUpdatedAt(Instant.now());
        }

        return mapped;
    }

    /**
     * Maps a laboratory Observation to HealthTwin and generates a LabResult entity.
     */
    public Optional<LabResult> mapLabObservation(Observation obs, HealthTwin twin, String patientId) {
        if (obs == null || twin == null || !obs.hasCode()) {
            return Optional.empty();
        }

        TwinLabs labs = twin.getLatestLabs() != null ? twin.getLatestLabs() : new TwinLabs();
        Instant timestamp = extractObservationInstant(obs);
        if (timestamp != null) {
            labs.setTimestamp(timestamp);
        }

        if (!obs.hasValueQuantity()) {
            return Optional.empty();
        }

        Quantity qty = obs.getValueQuantity();
        double val = qty.getValue().doubleValue();
        String unit = qty.hasUnit() ? qty.getUnit() : "";
        String testName = obs.getCode().hasText() ? obs.getCode().getText() : "Lab Test";
        String testCode = obs.getCode().getCodingFirstRep().getCode();

        boolean mapped = false;

        if (hasLoincCode(obs, LOINC_GLUCOSE, LOINC_GLUCOSE_ALT) || matchesText(obs, "glucose")) {
            labs.setGlucose(val);
            testName = "Glucose";
            mapped = true;
        } else if (hasLoincCode(obs, LOINC_CHOLESTEROL) || matchesText(obs, "cholesterol")) {
            labs.setCholesterol(val);
            testName = "Total Cholesterol";
            mapped = true;
        } else if (hasLoincCode(obs, LOINC_HEMOGLOBIN) || matchesText(obs, "hemoglobin")) {
            labs.setHemoglobin(val);
            testName = "Hemoglobin";
            mapped = true;
        } else if (hasLoincCode(obs, LOINC_CREATININE) || matchesText(obs, "creatinine")) {
            labs.setCreatinine(val);
            testName = "Creatinine";
            mapped = true;
        }

        twin.setLatestLabs(labs);
        twin.setUpdatedAt(Instant.now());

        // Extract reference range if present
        LabResult.ReferenceRange refRange = null;
        if (obs.hasReferenceRange()) {
            Observation.ObservationReferenceRangeComponent fhirRange = obs.getReferenceRangeFirstRep();
            Double low = fhirRange.hasLow() ? fhirRange.getLow().getValue().doubleValue() : null;
            Double high = fhirRange.hasHigh() ? fhirRange.getHigh().getValue().doubleValue() : null;
            refRange = new LabResult.ReferenceRange(low, high);
        }

        String status = obs.hasStatus() ? obs.getStatus().toCode().toUpperCase() : "FINAL";
        LabResult labResult = new LabResult(patientId, testName, testCode, val, unit, refRange, status, timestamp, "FHIR");
        return Optional.of(labResult);
    }

    private void recalculateBmi(TwinDemographics demo) {
        if (demo.getHeight() != null && demo.getHeight() > 0 && demo.getWeight() != null && demo.getWeight() > 0) {
            double heightM = demo.getHeight() / 100.0;
            double bmi = demo.getWeight() / (heightM * heightM);
            demo.setBmi(Math.round(bmi * 10.0) / 10.0);
        }
    }

    private Instant extractObservationInstant(Observation obs) {
        if (obs.hasEffectiveDateTimeType()) {
            DateTimeType dt = obs.getEffectiveDateTimeType();
            return dt.getValue().toInstant();
        } else if (obs.hasIssued()) {
            return obs.getIssued().toInstant();
        }
        return Instant.now();
    }

    private boolean hasLoincCode(Observation obs, String... codes) {
        return hasLoincCode(obs.getCode(), codes);
    }

    private boolean hasLoincCode(org.hl7.fhir.r4.model.CodeableConcept codeConcept, String... codes) {
        if (codeConcept == null || !codeConcept.hasCoding()) {
            return false;
        }
        for (Coding coding : codeConcept.getCoding()) {
            for (String target : codes) {
                if (target.equalsIgnoreCase(coding.getCode())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchesText(Observation obs, String... terms) {
        String text = obs.getCode().hasText() ? obs.getCode().getText().toLowerCase() : "";
        for (Coding c : obs.getCode().getCoding()) {
            if (c.hasDisplay()) {
                text += " " + c.getDisplay().toLowerCase();
            }
        }
        for (String term : terms) {
            if (text.contains(term.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Maps a FHIR R4 Consent resource into a domain Consent entity.
     * Returns Optional.empty() if the provision.actor provider cannot be resolved or is missing,
     * ensuring no domain records with null or unknown grantedTo are ever created.
     */
    public Optional<com.medisphere.consent.model.Consent> mapConsent(Consent fhirConsent, String patientId) {
        if (fhirConsent == null || !StringUtils.hasText(patientId)) {
            return Optional.empty();
        }

        // 1. Extract provider actor from provision
        String grantedTo = null;
        if (fhirConsent.hasProvision() && fhirConsent.getProvision().hasActor()) {
            for (var actor : fhirConsent.getProvision().getActor()) {
                if (actor.hasReference() && actor.getReference().hasReference()) {
                    String ref = actor.getReference().getReference();
                    if (ref.startsWith("Practitioner/")) {
                        grantedTo = ref.substring("Practitioner/".length()).trim();
                        break;
                    } else if (ref.startsWith("User/")) {
                        grantedTo = ref.substring("User/".length()).trim();
                        break;
                    } else if (StringUtils.hasText(ref)) {
                        grantedTo = ref.trim();
                        break;
                    }
                }
            }
        }

        if (!StringUtils.hasText(grantedTo)) {
            return Optional.empty();
        }

        // 2. Map status
        com.medisphere.consent.model.ConsentStatus domainStatus = com.medisphere.consent.model.ConsentStatus.GRANTED;
        if (fhirConsent.hasStatus()) {
            Consent.ConsentState state = fhirConsent.getStatus();
            if (state == Consent.ConsentState.ACTIVE) {
                domainStatus = com.medisphere.consent.model.ConsentStatus.GRANTED;
            } else if (state == Consent.ConsentState.INACTIVE
                    || state == Consent.ConsentState.REJECTED
                    || state == Consent.ConsentState.ENTEREDINERROR) {
                domainStatus = com.medisphere.consent.model.ConsentStatus.REVOKED;
            } else {
                domainStatus = com.medisphere.consent.model.ConsentStatus.PENDING;
            }
        }

        // 3. Map scope
        String scope = "treatment";
        if (fhirConsent.hasScope() && fhirConsent.getScope().hasCoding()
                && fhirConsent.getScope().getCodingFirstRep().hasCode()) {
            scope = fhirConsent.getScope().getCodingFirstRep().getCode();
        }

        // 4. Map dates
        Instant grantedAt = Instant.now();
        Instant expiresAt = null;
        if (fhirConsent.hasProvision() && fhirConsent.getProvision().hasPeriod()) {
            org.hl7.fhir.r4.model.Period period = fhirConsent.getProvision().getPeriod();
            if (period.hasStart()) {
                grantedAt = period.getStart().toInstant();
            }
            if (period.hasEnd()) {
                expiresAt = period.getEnd().toInstant();
            }
        } else if (fhirConsent.hasDateTime()) {
            grantedAt = fhirConsent.getDateTime().toInstant();
        }

        // 5. Map reason/policy
        String reason = null;
        if (fhirConsent.hasPolicy() && !fhirConsent.getPolicy().isEmpty()
                && fhirConsent.getPolicyFirstRep().hasUri()) {
            reason = fhirConsent.getPolicyFirstRep().getUri();
        }

        com.medisphere.consent.model.Consent domainConsent = new com.medisphere.consent.model.Consent(
                patientId, grantedTo, scope, domainStatus, grantedAt, expiresAt, reason
        );
        if (domainStatus == com.medisphere.consent.model.ConsentStatus.REVOKED) {
            domainConsent.setRevokedAt(Instant.now());
        }

        return Optional.of(domainConsent);
    }
}
