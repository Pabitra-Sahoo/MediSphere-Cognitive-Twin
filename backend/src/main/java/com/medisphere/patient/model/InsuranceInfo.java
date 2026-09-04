package com.medisphere.patient.model;

/**
 * Nested document representing patient insurance coverage.
 */
public class InsuranceInfo {

    private String provider;
    private String policyNumber;

    public InsuranceInfo() {
    }

    public InsuranceInfo(String provider, String policyNumber) {
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
