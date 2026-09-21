package com.proofchain.dto;

import java.util.List;

public class DashboardResponse {

    private Long developerId;
    private String developerName;
    private Integer overallProofScore;
    private int totalProjects;
    private int totalSkills;
    private int completedAnalyses;
    private List<ProofScoreResponse> skillProfiles;
    private List<ProjectResponse> projects;

    public DashboardResponse() {
    }

    public DashboardResponse(Long developerId, String developerName, Integer overallProofScore, int totalProjects, int totalSkills, int completedAnalyses, List<ProofScoreResponse> skillProfiles, List<ProjectResponse> projects) {
        this.developerId = developerId;
        this.developerName = developerName;
        this.overallProofScore = overallProofScore;
        this.totalProjects = totalProjects;
        this.totalSkills = totalSkills;
        this.completedAnalyses = completedAnalyses;
        this.skillProfiles = skillProfiles;
        this.projects = projects;
    }

    public Long getDeveloperId() {
        return developerId;
    }

    public String getDeveloperName() {
        return developerName;
    }

    public Integer getOverallProofScore() {
        return overallProofScore;
    }

    public int getTotalProjects() {
        return totalProjects;
    }

    public int getTotalSkills() {
        return totalSkills;
    }

    public int getCompletedAnalyses() {
        return completedAnalyses;
    }

    public List<ProofScoreResponse> getSkillProfiles() {
        return skillProfiles;
    }

    public List<ProjectResponse> getProjects() {
        return projects;
    }
}
