package com.medisphere.patient.dto;

public class InsuranceInfoDTO {

    private String provider;
    private String policyNumber;

    public InsuranceInfoDTO() {
    }

    public InsuranceInfoDTO(String provider, String policyNumber) {
        this.provider = provider;
        this.policyNumber = policyNumber;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }
}
