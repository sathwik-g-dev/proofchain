package com.proofchain.controller;

import com.proofchain.dto.EvidenceRequest;
import com.proofchain.dto.EvidenceResponse;
import com.proofchain.service.EvidenceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class EvidenceController {

    private final EvidenceService evidenceService;

    public EvidenceController(EvidenceService evidenceService) {
        this.evidenceService = evidenceService;
    }

    @PostMapping("/projects/{projectId}/evidence")
    public ResponseEntity<EvidenceResponse> addEvidence(
            @PathVariable("projectId") Long projectId,
            @Valid @RequestBody EvidenceRequest request) {
        EvidenceResponse response = evidenceService.addEvidence(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/projects/{projectId}/evidence/batch")
    public ResponseEntity<List<EvidenceResponse>> addEvidenceBatch(
            @PathVariable("projectId") Long projectId,
            @Valid @RequestBody List<EvidenceRequest> requests) {
        List<EvidenceResponse> responses = evidenceService.addEvidenceBatch(projectId, requests);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @GetMapping("/projects/{projectId}/evidence")
    public ResponseEntity<List<EvidenceResponse>> getProjectEvidence(@PathVariable("projectId") Long projectId) {
        return ResponseEntity.ok(evidenceService.getProjectEvidence(projectId));
    }

    @DeleteMapping("/evidence/{id}")
    public ResponseEntity<Void> deleteEvidence(@PathVariable("id") Long id) {
        evidenceService.deleteEvidence(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/projects/{projectId}/evidence")
    public ResponseEntity<Void> clearProjectEvidence(@PathVariable("projectId") Long projectId) {
        evidenceService.clearProjectEvidence(projectId);
        return ResponseEntity.noContent().build();
    }
}
