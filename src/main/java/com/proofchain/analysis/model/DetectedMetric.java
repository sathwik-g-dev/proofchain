package com.proofchain.analysis.model;

import java.util.ArrayList;
import java.util.List;

public class DetectedMetric {

    private String name;
    private String category;
    private String evidenceType;
    private int count;
    private String description;
    private List<String> contributingFiles = new ArrayList<>();

    public DetectedMetric() {
    }

    public DetectedMetric(String name, String category, String evidenceType, int count, String description, List<String> contributingFiles) {
        this.name = name;
        this.category = category;
        this.evidenceType = evidenceType;
        this.count = count;
        this.description = description;
        if (contributingFiles != null) {
            this.contributingFiles = contributingFiles;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getEvidenceType() {
        return evidenceType;
    }

    public void setEvidenceType(String evidenceType) {
        this.evidenceType = evidenceType;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getContributingFiles() {
        return contributingFiles;
    }

    public void setContributingFiles(List<String> contributingFiles) {
        this.contributingFiles = contributingFiles;
    }
}
