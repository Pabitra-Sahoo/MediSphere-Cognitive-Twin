package com.medisphere.twin.model;

/**
 * Nested sub-document representing patient demographics in the HealthTwin.
 */
public class TwinDemographics {

    private Integer age;
    private String gender;
    private Double bmi;
    private Double height;
    private Double weight;
    private String bloodType;

    public TwinDemographics() {
    }

    public TwinDemographics(Integer age, String gender, Double bmi, Double height, Double weight, String bloodType) {
        this.age = age;
        this.gender = gender;
        this.bmi = bmi;
        this.height = height;
        this.weight = weight;
        this.bloodType = bloodType;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Double getBmi() {
        return bmi;
    }

    public void setBmi(Double bmi) {
        this.bmi = bmi;
    }

    public Double getHeight() {
        return height;
    }

    public void setHeight(Double height) {
        this.height = height;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public String getBloodType() {
        return bloodType;
    }

    public void setBloodType(String bloodType) {
        this.bloodType = bloodType;
    }
}
