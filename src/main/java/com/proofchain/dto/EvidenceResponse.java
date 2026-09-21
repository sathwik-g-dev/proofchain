package com.proofchain.dto;

import java.time.LocalDateTime;

public class EvidenceResponse {

    private Long id;
    private Long projectId;
    private String projectName;
    private Long skillId;
    private String skillName;
    private String evidenceType;
    private String description;
    private Integer numericValue;
    private LocalDateTime createdAt;

    public EvidenceResponse() {
    }

    public EvidenceResponse(Long id, Long projectId, String projectName, Long skillId, String skillName, String evidenceType, String description, Integer numericValue, LocalDateTime createdAt) {
        this.id = id;
        this.projectId = projectId;
        this.projectName = projectName;
        this.skillId = skillId;
        this.skillName = skillName;
        this.evidenceType = evidenceType;
        this.description = description;
        this.numericValue = numericValue;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public Long getSkillId() {
        return skillId;
    }

    public String getSkillName() {
        return skillName;
    }

    public String getEvidenceType() {
        return evidenceType;
    }

    public String getDescription() {
        return description;
    }

    public Integer getNumericValue() {
        return numericValue;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
