package com.medisphere.patient.dto;

public class EmergencyContactDTO {

    private String name;
    private String phone;
    private String relationship;

    public EmergencyContactDTO() {
    }

    public EmergencyContactDTO(String name, String phone, String relationship) {
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
