package com.proofchain.analysis;

import com.proofchain.analysis.JavaSourceAnalyzer.JavaAnalysisData;
import com.proofchain.analysis.SpringBootAnalyzer.SpringAnalysisData;
import com.proofchain.analysis.TestAnalyzer.TestAnalysisData;
import com.proofchain.analysis.model.CodeStatistics;
import com.proofchain.analysis.model.DetectedMetric;
import com.proofchain.analysis.model.TechnologySummary;
import com.proofchain.dto.AnalysisReportResponse;
import com.proofchain.dto.ProjectReportResponse;
import com.proofchain.dto.ProofScoreResponse;
import com.proofchain.entity.Evidence;
import com.proofchain.entity.Project;
import com.proofchain.entity.ProofScore;
import com.proofchain.entity.Skill;
import com.proofchain.entity.User;
import com.proofchain.exception.ResourceNotFoundException;
import com.proofchain.repository.EvidenceRepository;
import com.proofchain.repository.ProjectRepository;
import com.proofchain.repository.ProofScoreRepository;
import com.proofchain.repository.SkillRepository;
import com.proofchain.repository.UserRepository;
import com.proofchain.service.EvidenceService;
import com.proofchain.service.ProofScoringEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
public class ProjectAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ProjectAnalysisService.class);

    private final ZipProjectExtractor zipProjectExtractor;
    private final GitHubProjectFetcher gitHubProjectFetcher;
    private final JavaSourceAnalyzer javaSourceAnalyzer;
    private final SpringBootAnalyzer springBootAnalyzer;
    private final TestAnalyzer testAnalyzer;
    private final BuildFileAnalyzer buildFileAnalyzer;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final EvidenceRepository evidenceRepository;
    private final ProofScoreRepository proofScoreRepository;
    private final ProofScoringEngine proofScoringEngine;
    private final EvidenceService evidenceService;

    public ProjectAnalysisService(ZipProjectExtractor zipProjectExtractor,
                                  GitHubProjectFetcher gitHubProjectFetcher,
                                  JavaSourceAnalyzer javaSourceAnalyzer,
                                  SpringBootAnalyzer springBootAnalyzer,
                                  TestAnalyzer testAnalyzer,
                                  BuildFileAnalyzer buildFileAnalyzer,
                                  ProjectRepository projectRepository,
                                  UserRepository userRepository,
                                  SkillRepository skillRepository,
                                  EvidenceRepository evidenceRepository,
                                  ProofScoreRepository proofScoreRepository,
                                  ProofScoringEngine proofScoringEngine,
                                  EvidenceService evidenceService) {
        this.zipProjectExtractor = zipProjectExtractor;
        this.gitHubProjectFetcher = gitHubProjectFetcher;
        this.javaSourceAnalyzer = javaSourceAnalyzer;
        this.springBootAnalyzer = springBootAnalyzer;
        this.testAnalyzer = testAnalyzer;
        this.buildFileAnalyzer = buildFileAnalyzer;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
        this.evidenceRepository = evidenceRepository;
        this.proofScoreRepository = proofScoreRepository;
        this.proofScoringEngine = proofScoringEngine;
        this.evidenceService = evidenceService;
    }

    /**
     * Analyzes an uploaded project ZIP file.
     */
    public AnalysisReportResponse analyzeUploadedZip(MultipartFile file,
                                                     String projectName,
                                                     String description,
                                                     String githubUrl,
                                                     Long userId) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("ZIP file cannot be empty");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".zip")) {
            throw new IllegalArgumentException("Uploaded file must be a valid .zip archive");
        }

        if (projectName == null || projectName.trim().isEmpty()) {
            projectName = originalName.substring(0, originalName.lastIndexOf('.'));
        }

        Path extractedDir = null;
        try {
            extractedDir = zipProjectExtractor.extract(file.getInputStream());
            return processExtractedProject(extractedDir, projectName.trim(), description, githubUrl, userId);
        } finally {
            if (extractedDir != null) {
                zipProjectExtractor.deleteDirectoryRecursively(extractedDir);
            }
        }
    }

    /**
     * Analyzes a public GitHub repository.
     */
    public AnalysisReportResponse analyzeGitHubRepository(String githubUrl,
                                                          String projectName,
                                                          String description,
                                                          Long userId) throws IOException {
        if (!gitHubProjectFetcher.isValidGitHubUrl(githubUrl)) {
            throw new IllegalArgumentException("Invalid GitHub repository URL: " + githubUrl);
        }

        String[] ownerRepo = gitHubProjectFetcher.extractOwnerAndRepo(githubUrl);
        if (projectName == null || projectName.trim().isEmpty()) {
            projectName = ownerRepo[1];
        }

        Path extractedDir = null;
        try {
            extractedDir = gitHubProjectFetcher.fetchAndExtract(githubUrl);
            return processExtractedProject(extractedDir, projectName.trim(), description, githubUrl.trim(), userId);
        } finally {
            if (extractedDir != null) {
                zipProjectExtractor.deleteDirectoryRecursively(extractedDir);
            }
        }
    }

    /**
     * Orchestrates AST analysis and scoring for an extracted directory.
     */
    private AnalysisReportResponse processExtractedProject(Path projectDir,
                                                           String projectName,
                                                           String description,
                                                           String githubUrl,
                                                           Long userId) throws IOException {
        int totalFiles = 0;
        try (Stream<Path> stream = Files.walk(projectDir)) {
            totalFiles = (int) stream.filter(Files::isRegularFile).count();
        }

        // 1. AST Java Analysis
        JavaAnalysisData javaData = javaSourceAnalyzer.analyze(projectDir);
        CodeStatistics stats = javaData.stats();
        stats.setTotalFiles(totalFiles);

        // 2. Build Tool Analysis
        TechnologySummary techSummary = buildFileAnalyzer.analyze(projectDir);

        // 3. Check if any Java files exist
        if (stats.getJavaFiles() == 0) {
            log.info("Project '{}' contains no Java files.", projectName);
            Project nonJavaProject = saveOrUpdateProject(projectName, description, githubUrl, userId, Set.of());
            AnalysisReportResponse emptyResponse = new AnalysisReportResponse();
            emptyResponse.setProjectId(nonJavaProject.getId());
            emptyResponse.setProjectName(projectName);
            emptyResponse.setDescription(description);
            emptyResponse.setGithubUrl(githubUrl);
            emptyResponse.setDeveloperName(nonJavaProject.getUser().getName());
            emptyResponse.setCreatedAt(nonJavaProject.getCreatedAt());
            emptyResponse.setJavaProject(false);
            emptyResponse.setStatusMessage("No supported Java source files were found in this project.");
            emptyResponse.setCodeStatistics(stats);
            emptyResponse.setTechnologySummary(techSummary);
            emptyResponse.setOverallProofScore(0);
            return emptyResponse;
        }

        // 4. Spring Boot & Testing Analyzers
        SpringAnalysisData springData = springBootAnalyzer.analyze(javaData.units(), stats);
        TestAnalysisData testData = testAnalyzer.analyze(javaData.units(), stats);

        // 5. Aggregate all detected metrics
        List<DetectedMetric> allMetrics = new ArrayList<>();
        allMetrics.addAll(javaData.metrics());
        allMetrics.addAll(springData.metrics());
        allMetrics.addAll(testData.metrics());

        // 6. Determine skills present
        Set<Skill> projectSkills = determineSkills(stats, springData.isSpringBootProject());

        // 7. Persist Project & Evidence
        Project project = saveOrUpdateProject(projectName, description, githubUrl, userId, projectSkills);

        // Clear existing evidence and proof scores for fresh recalculation
        evidenceService.clearProjectEvidence(project.getId());

        // Save new evidence items
        persistMetricsAsEvidence(project, allMetrics);

        // 8. Run ProofScoringEngine
        proofScoringEngine.analyzeProject(project.getId());
        ProjectReportResponse baseReport = proofScoringEngine.generateReport(project.getId());

        // 9. Assemble comprehensive AnalysisReportResponse
        AnalysisReportResponse response = new AnalysisReportResponse();
        response.setProjectId(project.getId());
        response.setProjectName(project.getName());
        response.setDescription(project.getDescription());
        response.setGithubUrl(project.getGithubUrl());
        response.setCreatedAt(project.getCreatedAt());
        response.setDeveloperName(project.getUser().getName());
        response.setOverallProofScore(baseReport.getOverallProofScore());
        response.setSkillScores(baseReport.getSkillScores());
        response.setEvidenceSummary(baseReport.getEvidenceSummary());
        response.setTopStrengths(baseReport.getTopStrengths());
        response.setKeyRecommendations(baseReport.getKeyRecommendations());
        response.setCodeStatistics(stats);
        response.setTechnologySummary(techSummary);
        response.setDetectedMetrics(allMetrics);
        response.setJavaProject(true);
        response.setStatusMessage(String.format("Analysis complete. Analyzed %d Java files and %d test methods.", stats.getJavaFiles(), stats.getTestMethodCount()));

        return response;
    }

    private Set<Skill> determineSkills(CodeStatistics stats, boolean isSpringBoot) {
        Set<Skill> skills = new HashSet<>();

        // Java is always present if java files were found
        findSkillByName("Java").ifPresent(skills::add);

        if (isSpringBoot || stats.getControllerCount() > 0 || stats.getServiceCount() > 0 || stats.getRepositoryCount() > 0) {
            findSkillByName("Spring Boot").ifPresent(skills::add);
        }

        if (stats.getEntityCount() > 0 || stats.getRepositoryCount() > 0) {
            findSkillByName("SQL").ifPresent(skills::add);
        }

        if (stats.getEndpointCount() > 0 || stats.getControllerCount() > 0) {
            findSkillByName("REST API").ifPresent(skills::add);
        }

        if (stats.getTestClassCount() > 0 || stats.getTestMethodCount() > 0) {
            findSkillByName("Testing").ifPresent(skills::add);
        }

        return skills;
    }

    private Optional<Skill> findSkillByName(String name) {
        return skillRepository.findByNameIgnoreCase(name);
    }

    private Project saveOrUpdateProject(String name, String description, String githubUrl, Long userId, Set<Skill> skills) {
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }
        if (user == null) {
            user = userRepository.findAll().stream().findFirst().orElseGet(() -> {
                User defaultUser = new User("Sathwik", "sathwik.dev@proofchain.io", "demoHash");
                return userRepository.save(defaultUser);
            });
        }

        Optional<Project> existing = projectRepository.findByUserId(user.getId())
                .stream()
                .filter(p -> p.getName().equalsIgnoreCase(name))
                .findFirst();

        Project project;
        if (existing.isPresent()) {
            project = existing.get();
            if (description != null && !description.isBlank()) {
                project.setDescription(description);
            }
            if (githubUrl != null && !githubUrl.isBlank()) {
                project.setGithubUrl(githubUrl);
            }
            project.setSkills(skills);
        } else {
            project = new Project(name, description, githubUrl, user);
            project.setSkills(skills);
        }

        return projectRepository.save(project);
    }

    private void persistMetricsAsEvidence(Project project, List<DetectedMetric> metrics) {
        List<Evidence> evidenceList = new ArrayList<>();

        for (DetectedMetric m : metrics) {
            String skillName = switch (m.getCategory().toUpperCase()) {
                case "JAVA" -> "Java";
                case "SPRING_BOOT" -> "Spring Boot";
                case "SQL" -> "SQL";
                case "REST_API" -> "REST API";
                case "TESTING" -> "Testing";
                default -> "Java";
            };

            Optional<Skill> skillOpt = findSkillByName(skillName);
            if (skillOpt.isEmpty() || !project.getSkills().contains(skillOpt.get())) {
                continue;
            }

            Skill skill = skillOpt.get();

            // Build evidence description, optionally with file references
            String desc = m.getDescription();
            if (!m.getContributingFiles().isEmpty()) {
                String filesSample = m.getContributingFiles().stream().limit(3).collect(Collectors.joining(", "));
                if (m.getContributingFiles().size() > 3) {
                    filesSample += String.format(" (+%d more)", m.getContributingFiles().size() - 3);
                }
                desc = desc + " | Files: " + filesSample;
            }

            if (desc.length() > 500) {
                desc = desc.substring(0, 497) + "...";
            }

            Evidence ev = new Evidence(
                    project,
                    skill,
                    m.getEvidenceType().trim().toUpperCase(),
                    desc,
                    m.getCount() > 0 ? m.getCount() : 1
            );
            evidenceList.add(ev);
        }

        evidenceRepository.saveAll(evidenceList);
    }

    /**
     * Retrieves the latest analysis report for a project.
     */
    @Transactional(readOnly = true)
    public AnalysisReportResponse getProjectAnalysis(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        ProjectReportResponse baseReport = proofScoringEngine.generateReport(projectId);
        List<Evidence> evidence = evidenceRepository.findByProjectId(projectId);

        AnalysisReportResponse response = new AnalysisReportResponse();
        response.setProjectId(project.getId());
        response.setProjectName(project.getName());
        response.setDescription(project.getDescription());
        response.setGithubUrl(project.getGithubUrl());
        response.setCreatedAt(project.getCreatedAt());
        response.setDeveloperName(project.getUser().getName());
        response.setOverallProofScore(baseReport.getOverallProofScore());
        response.setSkillScores(baseReport.getSkillScores());
        response.setEvidenceSummary(baseReport.getEvidenceSummary());
        response.setTopStrengths(baseReport.getTopStrengths());
        response.setKeyRecommendations(baseReport.getKeyRecommendations());
        response.setJavaProject(true);

        // Convert saved Evidence back into DetectedMetric list for transparent inspection
        List<DetectedMetric> metrics = new ArrayList<>();
        CodeStatistics stats = new CodeStatistics();

        Set<String> uniqueJavaFiles = new LinkedHashSet<>();
        boolean hasPom = false;
        boolean hasGradle = false;

        for (Evidence ev : evidence) {
            String cat = ev.getSkill().getName();
            String desc = ev.getDescription() != null ? ev.getDescription() : "";
            List<String> files = new ArrayList<>();
            if (desc.contains("| Files: ")) {
                String fileStr = desc.substring(desc.indexOf("| Files: ") + 9);
                for (String part : fileStr.split(",")) {
                    String clean = part.trim();
                    if (!clean.isEmpty() && !clean.startsWith("(+")) {
                        files.add(clean);
                        if (clean.endsWith(".java")) {
                            uniqueJavaFiles.add(clean);
                        }
                    }
                }
            }

            if (desc.toLowerCase().contains("pom.xml")) hasPom = true;
            if (desc.toLowerCase().contains("gradle")) hasGradle = true;

            int val = ev.getNumericValue() != null ? ev.getNumericValue() : 1;
            String type = ev.getEvidenceType();

            // Populate stats
            if (type.equals("JAVA_SOURCE_FILES") || type.equals("JAVA_FILES")) {
                stats.setJavaFiles(val);
            } else if (type.equals("DECLARED_METHODS") || type.equals("METHODS")) {
                stats.setMethodCount(val);
            } else if (type.equals("INTERFACES")) {
                stats.setInterfaceCount(val);
            } else if (type.contains("CLASS") && !type.contains("SOURCE")) {
                stats.setClassCount(val);
            }

            if (type.contains("CONTROLLER")) stats.setControllerCount(val);
            if (type.contains("ENDPOINT")) stats.setEndpointCount(val);
            if (type.contains("SERVICE")) stats.setServiceCount(val);
            if (type.contains("REPO")) stats.setRepositoryCount(val);
            if (type.contains("TABLE") || type.contains("ENTITY")) stats.setEntityCount(val);
            if (type.contains("UNIT_TEST")) stats.setTestMethodCount(val);

            metrics.add(new DetectedMetric(
                    ev.getEvidenceType(),
                    cat,
                    ev.getEvidenceType(),
                    val,
                    desc,
                    files
            ));
        }

        // Smart fallbacks if viewing legacy records:
        if (stats.getJavaFiles() == 0) {
            stats.setJavaFiles(Math.max(stats.getClassCount(), uniqueJavaFiles.size()));
        }
        if (stats.getMethodCount() == 0 && stats.getClassCount() > 0) {
            // Realistic approximation based on class and endpoint density
            stats.setMethodCount(Math.max(stats.getEndpointCount(), stats.getClassCount() * 3));
        }

        response.setDetectedMetrics(metrics);
        response.setCodeStatistics(stats);

        TechnologySummary tech = new TechnologySummary();
        tech.setPrimaryLanguage("Java");
        tech.setBuildTool(hasPom ? "Maven" : (hasGradle ? "Gradle" : "Maven"));
        tech.setFrameworks(project.getSkills().stream().map(Skill::getName).collect(Collectors.toList()));
        response.setTechnologySummary(tech);

        return response;
    }
}
