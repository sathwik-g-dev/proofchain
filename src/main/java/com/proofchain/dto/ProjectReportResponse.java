package com.proofchain.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ProjectReportResponse {

    private Long projectId;
    private String projectName;
    private String description;
    private String githubUrl;
    private LocalDateTime createdAt;
    private String developerName;
    private Integer overallProofScore;
    private List<ProofScoreResponse> skillScores;
    private Map<String, Integer> evidenceSummary;
    private List<String> topStrengths;
    private List<String> keyRecommendations;

    public ProjectReportResponse() {
    }

    public ProjectReportResponse(Long projectId, String projectName, String description, String githubUrl, LocalDateTime createdAt, String developerName, Integer overallProofScore, List<ProofScoreResponse> skillScores, Map<String, Integer> evidenceSummary, List<String> topStrengths, List<String> keyRecommendations) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.description = description;
        this.githubUrl = githubUrl;
        this.createdAt = createdAt;
        this.developerName = developerName;
        this.overallProofScore = overallProofScore;
        this.skillScores = skillScores;
        this.evidenceSummary = evidenceSummary;
        this.topStrengths = topStrengths;
        this.keyRecommendations = keyRecommendations;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getDescription() {
        return description;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getDeveloperName() {
        return developerName;
    }

    public Integer getOverallProofScore() {
        return overallProofScore;
    }

    public List<ProofScoreResponse> getSkillScores() {
        return skillScores;
    }

    public Map<String, Integer> getEvidenceSummary() {
        return evidenceSummary;
    }

    public List<String> getTopStrengths() {
        return topStrengths;
    }

    public List<String> getKeyRecommendations() {
        return keyRecommendations;
    }
}
