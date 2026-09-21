package com.proofchain.dto;

import jakarta.validation.constraints.NotBlank;

public class GitHubAnalysisRequest {

    @NotBlank(message = "GitHub repository URL is required")
    private String githubUrl;

    private String name;

    private String description;

    private Long userId;

    public GitHubAnalysisRequest() {
    }

    public GitHubAnalysisRequest(String githubUrl, String name, String description, Long userId) {
        this.githubUrl = githubUrl;
        this.name = name;
        this.description = description;
        this.userId = userId;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public void setGithubUrl(String githubUrl) {
        this.githubUrl = githubUrl;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
