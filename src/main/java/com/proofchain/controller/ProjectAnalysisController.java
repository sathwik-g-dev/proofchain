package com.proofchain.controller;

import com.proofchain.analysis.ProjectAnalysisService;
import com.proofchain.dto.AnalysisReportResponse;
import com.proofchain.dto.GitHubAnalysisRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/projects/analyze")
public class ProjectAnalysisController {

    private final ProjectAnalysisService projectAnalysisService;

    public ProjectAnalysisController(ProjectAnalysisService projectAnalysisService) {
        this.projectAnalysisService = projectAnalysisService;
    }

    /**
     * Upload and analyze a project ZIP file.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AnalysisReportResponse> analyzeZip(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "githubUrl", required = false) String githubUrl,
            @RequestParam(value = "userId", required = false) Long userId) throws IOException {

        AnalysisReportResponse response = projectAnalysisService.analyzeUploadedZip(
                file, name, description, githubUrl, userId
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Fetch and analyze a public GitHub repository.
     */
    @PostMapping("/github")
    public ResponseEntity<AnalysisReportResponse> analyzeGitHub(
            @Valid @RequestBody GitHubAnalysisRequest request) throws IOException {

        AnalysisReportResponse response = projectAnalysisService.analyzeGitHubRepository(
                request.getGithubUrl(),
                request.getName(),
                request.getDescription(),
                request.getUserId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieve the latest analysis report for a project.
     */
    @GetMapping("/{projectId}")
    public ResponseEntity<AnalysisReportResponse> getAnalysis(@PathVariable("projectId") Long projectId) {
        return ResponseEntity.ok(projectAnalysisService.getProjectAnalysis(projectId));
    }
}
