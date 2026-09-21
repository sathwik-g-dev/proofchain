package com.proofchain.controller;

import com.proofchain.dto.ProjectReportResponse;
import com.proofchain.dto.ProofScoreResponse;
import com.proofchain.service.ProofScoringEngine;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}")
public class ProofEngineController {

    private final ProofScoringEngine proofScoringEngine;

    public ProofEngineController(ProofScoringEngine proofScoringEngine) {
        this.proofScoringEngine = proofScoringEngine;
    }

    @PostMapping("/analyze")
    public ResponseEntity<List<ProofScoreResponse>> analyzeProject(@PathVariable("projectId") Long projectId) {
        List<ProofScoreResponse> scores = proofScoringEngine.analyzeProject(projectId);
        return ResponseEntity.ok(scores);
    }

    @GetMapping("/report")
    public ResponseEntity<ProjectReportResponse> getProjectReport(@PathVariable("projectId") Long projectId) {
        ProjectReportResponse report = proofScoringEngine.generateReport(projectId);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/scores")
    public ResponseEntity<List<ProofScoreResponse>> getProjectScores(@PathVariable("projectId") Long projectId) {
        List<ProofScoreResponse> scores = proofScoringEngine.getProjectScores(projectId);
        return ResponseEntity.ok(scores);
    }
}
