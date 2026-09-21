package com.proofchain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

public class ProjectRequest {

    @NotBlank(message = "Project name is required")
    @Size(min = 2, max = 150, message = "Project name must be between 2 and 150 characters")
    private String name;

    private String description;

    private String githubUrl;

    @NotNull(message = "User ID is required")
    private Long userId;

    private Set<Long> skillIds;

    public ProjectRequest() {
    }

    public ProjectRequest(String name, String description, String githubUrl, Long userId, Set<Long> skillIds) {
        this.name = name;
        this.description = description;
        this.githubUrl = githubUrl;
        this.userId = userId;
        this.skillIds = skillIds;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Set<Long> getSkillIds() {
        return skillIds;
    }

    public void setSkillIds(Set<Long> skillIds) {
        this.skillIds = skillIds;
    }
}
