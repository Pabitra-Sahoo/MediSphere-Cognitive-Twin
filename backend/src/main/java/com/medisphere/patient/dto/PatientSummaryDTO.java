package com.medisphere.patient.dto;

import java.time.LocalDate;

/**
 * Lightweight patient representation for patient list queries.
 */
public class PatientSummaryDTO {

    private String id;
    private String mrn;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String gender;
    private Double twinCompleteness;

    public PatientSummaryDTO() {
    }

    public PatientSummaryDTO(String id, String mrn, String firstName, String lastName,
                             LocalDate dateOfBirth, String gender, Double twinCompleteness) {
        this.id = id;
        this.mrn = mrn;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.twinCompleteness = twinCompleteness;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMrn() {
        return mrn;
    }

    public void setMrn(String mrn) {
        this.mrn = mrn;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Double getTwinCompleteness() {
        return twinCompleteness;
    }

    public void setTwinCompleteness(Double twinCompleteness) {
        this.twinCompleteness = twinCompleteness;
    }
}
