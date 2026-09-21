package com.proofchain.dto;

import java.time.LocalDateTime;

public class ProofScoreResponse {

    private Long id;
    private Long skillId;
    private String skillName;
    private Integer score;
    private String breakdown;
    private String strengths;
    private String recommendations;
    private LocalDateTime calculatedAt;

    public ProofScoreResponse() {
    }

    public ProofScoreResponse(Long id, Long skillId, String skillName, Integer score, String breakdown, String strengths, String recommendations, LocalDateTime calculatedAt) {
        this.id = id;
        this.skillId = skillId;
        this.skillName = skillName;
        this.score = score;
        this.breakdown = breakdown;
        this.strengths = strengths;
        this.recommendations = recommendations;
        this.calculatedAt = calculatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSkillId() {
        return skillId;
    }

    public void setSkillId(Long skillId) {
        this.skillId = skillId;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getBreakdown() {
        return breakdown;
    }

    public void setBreakdown(String breakdown) {
        this.breakdown = breakdown;
    }

    public String getStrengths() {
        return strengths;
    }

    public void setStrengths(String strengths) {
        this.strengths = strengths;
    }

    public String getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(String recommendations) {
        this.recommendations = recommendations;
    }

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(LocalDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
    }
}
