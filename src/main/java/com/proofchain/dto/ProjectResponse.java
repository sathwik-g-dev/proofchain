package com.proofchain.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ProjectResponse {

    private Long id;
    private String name;
    private String description;
    private String githubUrl;
    private LocalDateTime createdAt;
    private Long userId;
    private String userName;
    private List<String> skills;
    private int evidenceCount;
    private Integer overallScore;

    public ProjectResponse() {
    }

    public ProjectResponse(Long id, String name, String description, String githubUrl, LocalDateTime createdAt, Long userId, String userName, List<String> skills, int evidenceCount, Integer overallScore) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.githubUrl = githubUrl;
        this.createdAt = createdAt;
        this.userId = userId;
        this.userName = userName;
        this.skills = skills;
        this.evidenceCount = evidenceCount;
        this.overallScore = overallScore;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public int getEvidenceCount() {
        return evidenceCount;
    }

    public void setEvidenceCount(int evidenceCount) {
        this.evidenceCount = evidenceCount;
    }

    public Integer getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(Integer overallScore) {
        this.overallScore = overallScore;
    }
}
