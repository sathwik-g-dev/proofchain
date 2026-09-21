package com.proofchain.service;

import com.proofchain.dto.ProjectReportResponse;
import com.proofchain.dto.ProofScoreResponse;
import com.proofchain.entity.Evidence;
import com.proofchain.entity.Project;
import com.proofchain.entity.ProofScore;
import com.proofchain.entity.Skill;
import com.proofchain.exception.ResourceNotFoundException;
import com.proofchain.repository.EvidenceRepository;
import com.proofchain.repository.ProjectRepository;
import com.proofchain.repository.ProofScoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProofScoringEngine {

    private final ProjectRepository projectRepository;
    private final EvidenceRepository evidenceRepository;
    private final ProofScoreRepository proofScoreRepository;

    public ProofScoringEngine(ProjectRepository projectRepository,
                              EvidenceRepository evidenceRepository,
                              ProofScoreRepository proofScoreRepository) {
        this.projectRepository = projectRepository;
        this.evidenceRepository = evidenceRepository;
        this.proofScoreRepository = proofScoreRepository;
    }

    public List<ProofScoreResponse> analyzeProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        List<ProofScoreResponse> results = new ArrayList<>();

        for (Skill skill : project.getSkills()) {
            List<Evidence> evidenceList = evidenceRepository.findByProjectIdAndSkillId(projectId, skill.getId());
            ScoreResult evaluation = evaluateSkill(skill.getName(), evidenceList);

            Optional<ProofScore> existingScore = proofScoreRepository.findByProjectIdAndSkillId(projectId, skill.getId());
            ProofScore proofScore;
            if (existingScore.isPresent()) {
                proofScore = existingScore.get();
                proofScore.setScore(evaluation.score);
                proofScore.setBreakdown(evaluation.breakdown);
                proofScore.setStrengths(evaluation.strengths);
                proofScore.setRecommendations(evaluation.recommendations);
                proofScore.setCalculatedAt(LocalDateTime.now());
            } else {
                proofScore = new ProofScore(
                        project,
                        skill,
                        evaluation.score,
                        evaluation.breakdown,
                        evaluation.strengths,
                        evaluation.recommendations
                );
            }

            ProofScore saved = proofScoreRepository.save(proofScore);
            results.add(mapToResponse(saved));
        }

        return results;
    }

    @Transactional
    public ProjectReportResponse generateReport(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        List<ProofScore> scores = proofScoreRepository.findByProjectId(projectId);
        if (scores.isEmpty()) {
            // Automatically run analysis if not calculated yet
            analyzeProject(projectId);
            scores = proofScoreRepository.findByProjectId(projectId);
        }

        int overallScore = 0;
        if (!scores.isEmpty()) {
            int sum = scores.stream().mapToInt(ProofScore::getScore).sum();
            overallScore = Math.round((float) sum / scores.size());
        }

        Map<String, Integer> evidenceSummary = new LinkedHashMap<>();
        List<Object[]> summaryRows = evidenceRepository.getEvidenceSummaryByProjectId(projectId);
        for (Object[] row : summaryRows) {
            String type = (String) row[0];
            Number sum = (Number) row[1];
            evidenceSummary.put(type, sum != null ? sum.intValue() : 0);
        }

        List<String> topStrengths = new ArrayList<>();
        List<String> keyRecommendations = new ArrayList<>();

        for (ProofScore s : scores) {
            if (s.getStrengths() != null && !s.getStrengths().isBlank()) {
                for (String str : s.getStrengths().split(";")) {
                    if (!str.trim().isEmpty() && !topStrengths.contains(str.trim())) {
                        topStrengths.add(str.trim());
                    }
                }
            }
            if (s.getRecommendations() != null && !s.getRecommendations().isBlank()) {
                for (String rec : s.getRecommendations().split(";")) {
                    if (!rec.trim().isEmpty() && !keyRecommendations.contains(rec.trim())) {
                        keyRecommendations.add(rec.trim());
                    }
                }
            }
        }

        List<ProofScoreResponse> scoreResponses = scores.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new ProjectReportResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getGithubUrl(),
                project.getCreatedAt(),
                project.getUser().getName(),
                overallScore,
                scoreResponses,
                evidenceSummary,
                topStrengths,
                keyRecommendations
        );
    }

    @Transactional(readOnly = true)
    public List<ProofScoreResponse> getProjectScores(Long projectId) {
        return proofScoreRepository.findByProjectId(projectId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ScoreResult evaluateSkill(String skillName, List<Evidence> evidenceList) {
        String normalized = skillName.trim().toUpperCase();

        if (normalized.contains("JAVA") && !normalized.contains("SCRIPT")) {
            return scoreJava(evidenceList);
        } else if (normalized.contains("SPRING")) {
            return scoreSpringBoot(evidenceList);
        } else if (normalized.contains("SQL") || normalized.contains("DATABASE")) {
            return scoreSql(evidenceList);
        } else if (normalized.contains("REST") || normalized.contains("API")) {
            return scoreRestApi(evidenceList);
        } else if (normalized.contains("TEST")) {
            return scoreTesting(evidenceList);
        } else {
            return scoreGeneric(skillName, evidenceList);
        }
    }

    private ScoreResult scoreJava(List<Evidence> evidence) {
        int oop = 0, collections = 0, exceptions = 0, businessLogic = 0, testing = 0, complexity = 0;
        List<String> strengths = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        for (Evidence ev : evidence) {
            String type = ev.getEvidenceType().toUpperCase();
            int val = ev.getNumericValue() != null ? ev.getNumericValue() : 1;

            if (type.contains("OOP") || type.contains("INHERITANCE") || type.contains("POLYMORPHISM")) {
                oop = Math.min(20, oop + (val > 0 ? 20 : 10));
                strengths.add("Robust OOP architecture with clear abstraction hierarchy");
            } else if (type.contains("COLLECTION") || type.contains("STREAM") || type.contains("LIST") || type.contains("MAP")) {
                collections = Math.min(15, collections + Math.min(15, val * 3));
                strengths.add("Strong usage of Java Collections Framework and Stream API");
            } else if (type.contains("EXCEPTION") || type.contains("ERROR")) {
                exceptions = Math.min(10, exceptions + 10);
                strengths.add("Custom exception hierarchy with robust failure handling");
            } else if (type.contains("SERVICE") || type.contains("BUSINESS") || type.contains("LOGIC")) {
                businessLogic = Math.min(25, businessLogic + Math.min(25, val * 5));
                strengths.add("Decoupled business service layer separating concerns");
            } else if (type.contains("TEST") || type.contains("JUNIT")) {
                testing = Math.min(15, testing + Math.min(15, val * 2));
                strengths.add("Automated unit testing with test coverage evidence");
            } else if (type.contains("CLASS") || type.contains("MODULE")) {
                complexity = Math.min(15, complexity + Math.min(15, val));
            }
        }

        // Default points for present evidence
        if (complexity == 0 && !evidence.isEmpty()) complexity = 10;

        int total = oop + collections + exceptions + businessLogic + testing + complexity;

        if (testing == 0) recommendations.add("Add JUnit / Mockito unit test evidence to boost score by up to +15 pts");
        if (exceptions == 0) recommendations.add("Document custom exception handling to gain +10 pts");
        if (oop == 0) recommendations.add("Specify OOP concepts applied (polymorphism, encapsulation) for +20 pts");

        String breakdown = String.format("OOP: %d/20 | Collections: %d/15 | Exceptions: %d/10 | Business Logic: %d/25 | Testing: %d/15 | Project Complexity: %d/15",
                oop, collections, exceptions, businessLogic, testing, complexity);

        return new ScoreResult(total, breakdown, String.join("; ", strengths), String.join("; ", recommendations));
    }

    private ScoreResult scoreSpringBoot(List<Evidence> evidence) {
        int controllers = 0, endpoints = 0, services = 0, repos = 0, validation = 0, exceptionHandler = 0, complexity = 0;
        List<String> strengths = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        for (Evidence ev : evidence) {
            String type = ev.getEvidenceType().toUpperCase();
            int val = ev.getNumericValue() != null ? ev.getNumericValue() : 1;

            if (type.contains("CONTROLLER")) {
                controllers = Math.min(15, controllers + 15);
                strengths.add("Clean REST controller layer handling HTTP request mapping");
            } else if (type.contains("ENDPOINT") || type.contains("API")) {
                endpoints = Math.min(20, endpoints + Math.min(20, val * 3));
                strengths.add(String.format("Comprehensive REST API surface (%d registered endpoints)", val));
            } else if (type.contains("SERVICE")) {
                services = Math.min(15, services + 15);
                strengths.add("Dependency injection with Spring @Service business layer");
            } else if (type.contains("REPO") || type.contains("JPA") || type.contains("HIBERNATE")) {
                repos = Math.min(15, repos + 15);
                strengths.add("Data access layer leveraging Spring Data JPA & repositories");
            } else if (type.contains("VALIDATION") || type.contains("VALID")) {
                validation = Math.min(10, validation + 10);
                strengths.add("Bean Validation (@Valid, @NotNull, @NotBlank) securing input data");
            } else if (type.contains("ADVICE") || type.contains("EXCEPTION")) {
                exceptionHandler = Math.min(10, exceptionHandler + 10);
                strengths.add("Centralized @RestControllerAdvice error interception");
            }
        }

        complexity = Math.min(15, (endpoints > 0 ? 10 : 0) + (repos > 0 ? 5 : 0));
        int total = controllers + endpoints + services + repos + validation + exceptionHandler + complexity;

        if (validation == 0) recommendations.add("Document input validation (@Valid, constraints) to earn +10 pts");
        if (exceptionHandler == 0) recommendations.add("Register @RestControllerAdvice exception handler for +10 pts");

        String breakdown = String.format("Controllers: %d/15 | Endpoints: %d/20 | Service Layer: %d/15 | JPA Repository: %d/15 | Validation: %d/10 | Exception Handler: %d/10 | Project Depth: %d/15",
                controllers, endpoints, services, repos, validation, exceptionHandler, complexity);

        return new ScoreResult(total, breakdown, String.join("; ", strengths), String.join("; ", recommendations));
    }

    private ScoreResult scoreSql(List<Evidence> evidence) {
        int tables = 0, relations = 0, joins = 0, constraints = 0, transactions = 0, indexes = 0;
        List<String> strengths = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        for (Evidence ev : evidence) {
            String type = ev.getEvidenceType().toUpperCase();
            int val = ev.getNumericValue() != null ? ev.getNumericValue() : 1;

            if (type.contains("TABLE")) {
                tables = Math.min(15, tables + Math.min(15, val * 3));
                strengths.add(String.format("Normalized database schema with %d tables", val));
            } else if (type.contains("RELATION") || type.contains("FOREIGN") || type.contains("KEY")) {
                relations = Math.min(20, relations + 20);
                strengths.add("Proper relational integrity via foreign key associations");
            } else if (type.contains("JOIN") || type.contains("QUERY")) {
                joins = Math.min(20, joins + Math.min(20, val * 5));
                strengths.add("Multi-table relational queries and JOIN operations documented");
            } else if (type.contains("CONSTRAINT") || type.contains("UNIQUE")) {
                constraints = Math.min(15, constraints + 15);
                strengths.add("Domain validation enforced through database constraints");
            } else if (type.contains("TRANSACTION") || type.contains("ACID")) {
                transactions = Math.min(15, transactions + 15);
                strengths.add("ACID compliance guaranteed via declarative transaction management");
            } else if (type.contains("INDEX")) {
                indexes = Math.min(15, indexes + 15);
                strengths.add("B-Tree query performance optimization with indexing");
            }
        }

        int total = tables + relations + joins + constraints + transactions + indexes;

        if (relations == 0) recommendations.add("Document foreign key relationships (1:N, N:M) for +20 pts");
        if (joins == 0) recommendations.add("Detail JOIN query complexity for +20 pts");
        if (indexes == 0) recommendations.add("Document database indexes for performance optimization (+15 pts)");

        String breakdown = String.format("Tables: %d/15 | Relationships: %d/20 | JOINs: %d/20 | Constraints: %d/15 | Transactions: %d/15 | Indexes: %d/15",
                tables, relations, joins, constraints, transactions, indexes);

        return new ScoreResult(total, breakdown, String.join("; ", strengths), String.join("; ", recommendations));
    }

    private ScoreResult scoreRestApi(List<Evidence> evidence) {
        int endpoints = 0, statusCodes = 0, dtos = 0, errorFormat = 0, docs = 0;
        List<String> strengths = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        for (Evidence ev : evidence) {
            String type = ev.getEvidenceType().toUpperCase();
            int val = ev.getNumericValue() != null ? ev.getNumericValue() : 1;

            if (type.contains("ENDPOINT") || type.contains("VERB")) {
                endpoints = Math.min(25, endpoints + Math.min(25, val * 4));
                strengths.add("RESTful URI design covering full HTTP lifecycle (GET, POST, PUT, DELETE)");
            } else if (type.contains("STATUS") || type.contains("CODE")) {
                statusCodes = Math.min(20, statusCodes + 20);
                strengths.add("Semantic HTTP status codes (200 OK, 201 Created, 400 Bad Request, 404 Not Found)");
            } else if (type.contains("DTO") || type.contains("REQUEST") || type.contains("RESPONSE")) {
                dtos = Math.min(20, dtos + 20);
                strengths.add("Strict DTO boundary preventing mass assignment vulnerabilities");
            } else if (type.contains("ERROR") || type.contains("ADVICE")) {
                errorFormat = Math.min(15, errorFormat + 15);
                strengths.add("Structured RFC-compliant JSON error responses");
            } else if (type.contains("POSTMAN") || type.contains("DOC") || type.contains("SWAGGER")) {
                docs = Math.min(20, docs + 20);
                strengths.add("Verified API collection with documentation / Postman testing");
            }
        }

        int total = endpoints + statusCodes + dtos + errorFormat + docs;
        if (dtos == 0) recommendations.add("Document DTO separation for request/response bodies (+20 pts)");
        if (docs == 0) recommendations.add("Provide Postman or OpenAPI documentation evidence (+20 pts)");

        String breakdown = String.format("Endpoints: %d/25 | Status Codes: %d/20 | DTO Pattern: %d/20 | Error Format: %d/15 | API Docs: %d/20",
                endpoints, statusCodes, dtos, errorFormat, docs);

        return new ScoreResult(total, breakdown, String.join("; ", strengths), String.join("; ", recommendations));
    }

    private ScoreResult scoreTesting(List<Evidence> evidence) {
        int unitTests = 0, mocking = 0, coverage = 0, integration = 0;
        List<String> strengths = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        for (Evidence ev : evidence) {
            String type = ev.getEvidenceType().toUpperCase();
            int val = ev.getNumericValue() != null ? ev.getNumericValue() : 1;

            if (type.contains("UNIT") || type.contains("JUNIT")) {
                unitTests = Math.min(35, unitTests + Math.min(35, val * 3));
                strengths.add(String.format("Suite of %d automated unit tests verifying isolated behavior", val));
            } else if (type.contains("MOCK") || type.contains("MOCKITO")) {
                mocking = Math.min(25, mocking + 25);
                strengths.add("Isolated unit testing using Mockito dependency mocking");
            } else if (type.contains("COVERAGE") || type.contains("ASSERT")) {
                coverage = Math.min(20, coverage + 20);
                strengths.add("Verification of boundary edge cases and failure paths");
            } else if (type.contains("INTEGRATION") || type.contains("END_TO_END")) {
                integration = Math.min(20, integration + 20);
                strengths.add("End-to-end integration tests verifying cross-component orchestration");
            }
        }

        int total = unitTests + mocking + coverage + integration;
        if (mocking == 0) recommendations.add("Incorporate Mockito stubbing for service isolation (+25 pts)");
        if (unitTests < 15) recommendations.add("Increase unit test count to cover additional service paths (+15 pts)");

        String breakdown = String.format("Unit Tests: %d/35 | Mockito Mocking: %d/25 | Edge Coverage: %d/20 | Integration Tests: %d/20",
                unitTests, mocking, coverage, integration);

        return new ScoreResult(total, breakdown, String.join("; ", strengths), String.join("; ", recommendations));
    }

    private ScoreResult scoreGeneric(String skillName, List<Evidence> evidence) {
        int count = evidence.size();
        int totalValue = evidence.stream().mapToInt(e -> e.getNumericValue() != null ? e.getNumericValue() : 1).sum();

        int score = Math.min(95, Math.max(25, count * 20 + Math.min(40, totalValue * 2)));

        String breakdown = String.format("Evidence Items: %d registered | Value Metric: %d | Documentation Completeness: %d%%",
                count, totalValue, Math.min(100, count * 30));

        String strengths = count > 0 ? "Registered practical project implementation evidence for " + skillName : "Basic usage evidence registered";
        String recommendations = count < 3 ? "Register additional quantitative metrics to increase proof confidence" : "Maintain code evidence documentation";

        return new ScoreResult(score, breakdown, strengths, recommendations);
    }

    private ProofScoreResponse mapToResponse(ProofScore ps) {
        return new ProofScoreResponse(
                ps.getId(),
                ps.getSkill().getId(),
                ps.getSkill().getName(),
                ps.getScore(),
                ps.getBreakdown(),
                ps.getStrengths(),
                ps.getRecommendations(),
                ps.getCalculatedAt()
        );
    }

    private static class ScoreResult {
        final int score;
        final String breakdown;
        final String strengths;
        final String recommendations;

        ScoreResult(int score, String breakdown, String strengths, String recommendations) {
            this.score = Math.min(100, Math.max(0, score));
            this.breakdown = breakdown;
            this.strengths = strengths;
            this.recommendations = recommendations;
        }
    }
}
