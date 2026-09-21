package com.proofchain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class EvidenceRequest {

    @NotNull(message = "Skill ID is required")
    private Long skillId;

    @NotBlank(message = "Evidence type is required (e.g., JAVA_CLASSES, REST_ENDPOINTS, UNIT_TESTS)")
    private String evidenceType;

    private String description;

    @Min(value = 0, message = "Numeric value cannot be negative")
    private Integer numericValue;

    public EvidenceRequest() {
    }

    public EvidenceRequest(Long skillId, String evidenceType, String description, Integer numericValue) {
        this.skillId = skillId;
        this.evidenceType = evidenceType;
        this.description = description;
        this.numericValue = numericValue;
    }

    public Long getSkillId() {
        return skillId;
    }

    public void setSkillId(Long skillId) {
        this.skillId = skillId;
    }

    public String getEvidenceType() {
        return evidenceType;
    }

    public void setEvidenceType(String evidenceType) {
        this.evidenceType = evidenceType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getNumericValue() {
        return numericValue;
    }

    public void setNumericValue(Integer numericValue) {
        this.numericValue = numericValue;
    }
}
