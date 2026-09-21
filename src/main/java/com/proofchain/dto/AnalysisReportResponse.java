package com.proofchain.dto;

import com.proofchain.analysis.model.CodeStatistics;
import com.proofchain.analysis.model.DetectedMetric;
import com.proofchain.analysis.model.TechnologySummary;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AnalysisReportResponse {

    private Long projectId;
    private String projectName;
    private String description;
    private String githubUrl;
    private LocalDateTime createdAt;
    private String developerName;
    private Integer overallProofScore;
    private List<ProofScoreResponse> skillScores = new ArrayList<>();
    private Map<String, Integer> evidenceSummary = new LinkedHashMap<>();
    private List<String> topStrengths = new ArrayList<>();
    private List<String> keyRecommendations = new ArrayList<>();
    private CodeStatistics codeStatistics = new CodeStatistics();
    private TechnologySummary technologySummary = new TechnologySummary();
    private List<DetectedMetric> detectedMetrics = new ArrayList<>();
    private boolean isJavaProject = true;
    private String statusMessage;

    public AnalysisReportResponse() {
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public void setGithubUrl(String githubUrl) {
        this.githubUrl = githubUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getDeveloperName() {
        return developerName;
    }

    public void setDeveloperName(String developerName) {
        this.developerName = developerName;
    }

    public Integer getOverallProofScore() {
        return overallProofScore;
    }

    public void setOverallProofScore(Integer overallProofScore) {
        this.overallProofScore = overallProofScore;
    }

    public List<ProofScoreResponse> getSkillScores() {
        return skillScores;
    }

    public void setSkillScores(List<ProofScoreResponse> skillScores) {
        this.skillScores = skillScores;
    }

    public Map<String, Integer> getEvidenceSummary() {
        return evidenceSummary;
    }

    public void setEvidenceSummary(Map<String, Integer> evidenceSummary) {
        this.evidenceSummary = evidenceSummary;
    }

    public List<String> getTopStrengths() {
        return topStrengths;
    }

    public void setTopStrengths(List<String> topStrengths) {
        this.topStrengths = topStrengths;
    }

    public List<String> getKeyRecommendations() {
        return keyRecommendations;
    }

    public void setKeyRecommendations(List<String> keyRecommendations) {
        this.keyRecommendations = keyRecommendations;
    }

    public CodeStatistics getCodeStatistics() {
        return codeStatistics;
    }

    public void setCodeStatistics(CodeStatistics codeStatistics) {
        this.codeStatistics = codeStatistics;
    }

    public TechnologySummary getTechnologySummary() {
        return technologySummary;
    }

    public void setTechnologySummary(TechnologySummary technologySummary) {
        this.technologySummary = technologySummary;
    }

    public List<DetectedMetric> getDetectedMetrics() {
        return detectedMetrics;
    }

    public void setDetectedMetrics(List<DetectedMetric> detectedMetrics) {
        this.detectedMetrics = detectedMetrics;
    }

    public boolean isJavaProject() {
        return isJavaProject;
    }

    public void setJavaProject(boolean javaProject) {
        isJavaProject = javaProject;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
}
