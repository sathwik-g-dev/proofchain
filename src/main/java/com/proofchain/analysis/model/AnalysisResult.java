package com.proofchain.analysis.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AnalysisResult {

    private boolean javaProject;
    private CodeStatistics codeStatistics = new CodeStatistics();
    private TechnologySummary technologySummary = new TechnologySummary();
    private List<DetectedMetric> detectedMetrics = new ArrayList<>();
    private LocalDateTime analyzedAt = LocalDateTime.now();
    private String statusMessage;

    public AnalysisResult() {
    }

    public boolean isJavaProject() {
        return javaProject;
    }

    public void setJavaProject(boolean javaProject) {
        this.javaProject = javaProject;
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

    public LocalDateTime getAnalyzedAt() {
        return analyzedAt;
    }

    public void setAnalyzedAt(LocalDateTime analyzedAt) {
        this.analyzedAt = analyzedAt;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
}
