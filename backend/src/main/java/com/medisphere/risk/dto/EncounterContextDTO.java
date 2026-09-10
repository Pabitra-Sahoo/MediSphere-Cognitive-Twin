package com.medisphere.risk.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Explicit clinical inpatient encounter context required for Diabetes Complications risk estimation.
 * Captures encounter measurements that are not part of routine baseline vitals/demographics.
 */
public class EncounterContextDTO {

    @NotNull(message = "timeInHospital is required")
    @Min(value = 1, message = "timeInHospital must be at least 1 day")
    @Max(value = 14, message = "timeInHospital cannot exceed 14 days")
    private Integer timeInHospital;

    @NotNull(message = "numLabProcedures is required")
    @Min(value = 1, message = "numLabProcedures must be at least 1")
    @Max(value = 132, message = "numLabProcedures cannot exceed 132")
    private Integer numLabProcedures;

    @NotNull(message = "numProcedures is required")
    @Min(value = 0, message = "numProcedures cannot be negative")
    @Max(value = 6, message = "numProcedures cannot exceed 6")
    private Integer numProcedures;

    @NotNull(message = "numMedications is required")
    @Min(value = 1, message = "numMedications must be at least 1")
    @Max(value = 81, message = "numMedications cannot exceed 81")
    private Integer numMedications;

    @NotNull(message = "numberDiagnoses is required")
    @Min(value = 1, message = "numberDiagnoses must be at least 1")
    @Max(value = 16, message = "numberDiagnoses cannot exceed 16")
    private Integer numberDiagnoses;

    @NotBlank(message = "maxGluSerum is required (e.g. none, norm, >200, >300)")
    private String maxGluSerum;

    @NotBlank(message = "a1cResult is required (e.g. none, norm, >7, >8)")
    private String a1cResult;

    @NotBlank(message = "insulin is required (e.g. no, down, steady, up)")
    private String insulin;

    @NotBlank(message = "diabetesMed is required (e.g. no, yes)")
    private String diabetesMed;

    public EncounterContextDTO() {
    }

    public EncounterContextDTO(Integer timeInHospital, Integer numLabProcedures, Integer numProcedures,
                               Integer numMedications, Integer numberDiagnoses, String maxGluSerum,
                               String a1cResult, String insulin, String diabetesMed) {
        this.timeInHospital = timeInHospital;
        this.numLabProcedures = numLabProcedures;
        this.numProcedures = numProcedures;
        this.numMedications = numMedications;
        this.numberDiagnoses = numberDiagnoses;
        this.maxGluSerum = maxGluSerum;
        this.a1cResult = a1cResult;
        this.insulin = insulin;
        this.diabetesMed = diabetesMed;
    }

    public Integer getTimeInHospital() {
        return timeInHospital;
    }

    public void setTimeInHospital(Integer timeInHospital) {
        this.timeInHospital = timeInHospital;
    }

    public Integer getNumLabProcedures() {
        return numLabProcedures;
    }

    public void setNumLabProcedures(Integer numLabProcedures) {
        this.numLabProcedures = numLabProcedures;
    }

    public Integer getNumProcedures() {
        return numProcedures;
    }

    public void setNumProcedures(Integer numProcedures) {
        this.numProcedures = numProcedures;
    }

    public Integer getNumMedications() {
        return numMedications;
    }

    public void setNumMedications(Integer numMedications) {
        this.numMedications = numMedications;
    }

    public Integer getNumberDiagnoses() {
        return numberDiagnoses;
    }

    public void setNumberDiagnoses(Integer numberDiagnoses) {
        this.numberDiagnoses = numberDiagnoses;
    }

    public String getMaxGluSerum() {
        return maxGluSerum;
    }

    public void setMaxGluSerum(String maxGluSerum) {
        this.maxGluSerum = maxGluSerum;
    }

    public String getA1cResult() {
        return a1cResult;
    }

    public void setA1cResult(String a1cResult) {
        this.a1cResult = a1cResult;
    }

    public String getInsulin() {
        return insulin;
    }

    public void setInsulin(String insulin) {
        this.insulin = insulin;
    }

    public String getDiabetesMed() {
        return diabetesMed;
    }

    public void setDiabetesMed(String diabetesMed) {
        this.diabetesMed = diabetesMed;
    }
}
