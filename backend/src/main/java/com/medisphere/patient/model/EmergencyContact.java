package com.medisphere.patient.model;

/**
 * Nested document representing emergency contact details.
 * Counts as 1 logical completeness field when populated with name and phone.
 */
public class EmergencyContact {

    private String name;
    private String phone;
    private String relationship;

    public EmergencyContact() {
    }

    public EmergencyContact(String name, String phone, String relationship) {
        this.name = name;
        this.phone = phone;
        this.relationship = relationship;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }
}
