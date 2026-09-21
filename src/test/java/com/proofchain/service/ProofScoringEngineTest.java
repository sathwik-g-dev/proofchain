package com.proofchain.service;

import com.proofchain.dto.ProofScoreResponse;
import com.proofchain.entity.Evidence;
import com.proofchain.entity.Project;
import com.proofchain.entity.ProofScore;
import com.proofchain.entity.Skill;
import com.proofchain.entity.User;
import com.proofchain.repository.EvidenceRepository;
import com.proofchain.repository.ProjectRepository;
import com.proofchain.repository.ProofScoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProofScoringEngineTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private EvidenceRepository evidenceRepository;

    @Mock
    private ProofScoreRepository proofScoreRepository;

    @InjectMocks
    private ProofScoringEngine proofScoringEngine;

    private User sampleUser;
    private Project sampleProject;
    private Skill javaSkill;
    private Skill springBootSkill;
    private Skill sqlSkill;
    private Skill restApiSkill;
    private Skill testingSkill;
    private Skill genericSkill;

    @BeforeEach
    void setUp() {
        sampleUser = new User("Sathwik", "sathwik@proofchain.io", "hashedPass123");
        sampleProject = new Project("ProofChain", "Skill verification platform", "https://github.com/sathwik/proofchain", sampleUser);

        javaSkill = new Skill("Java", "Core Java language", "LANGUAGE");
        springBootSkill = new Skill("Spring Boot", "Enterprise Spring framework", "FRAMEWORK");
        sqlSkill = new Skill("SQL", "Relational database schema and queries", "DATABASE");
        restApiSkill = new Skill("REST API", "HTTP verbs, status codes, DTOs", "ARCHITECTURE");
        testingSkill = new Skill("Testing", "JUnit 5 and Mockito", "QUALITY");
        genericSkill = new Skill("Docker", "Containerization", "DEVOPS");

        lenient().when(proofScoreRepository.save(any(ProofScore.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Evaluate Java skill with comprehensive evidence awards high score and breakdown")
    void testScoreJava_withComprehensiveEvidence_calculatesExpectedBreakdownAndStrengths() {
        sampleProject.setSkills(Set.of(javaSkill));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));

        List<Evidence> javaEvidence = List.of(
                new Evidence(sampleProject, javaSkill, "OOP_CONCEPTS", "Encapsulation & Polymorphism", 4),
                new Evidence(sampleProject, javaSkill, "COLLECTIONS_FRAMEWORK", "Stream API & Lists", 8),
                new Evidence(sampleProject, javaSkill, "EXCEPTION_HANDLING", "Custom checked/unchecked hierarchy", 2),
                new Evidence(sampleProject, javaSkill, "BUSINESS_LOGIC_MODULES", "Core scoring evaluation service", 4),
                new Evidence(sampleProject, javaSkill, "UNIT_TESTS", "Isolated service unit tests", 14)
        );
        when(evidenceRepository.findByProjectIdAndSkillId(1L, javaSkill.getId())).thenReturn(javaEvidence);

        List<ProofScoreResponse> scores = proofScoringEngine.analyzeProject(1L);

        assertEquals(1, scores.size());
        ProofScoreResponse javaScore = scores.get(0);
        assertTrue(javaScore.getScore() >= 80, "Comprehensive Java evidence should yield score >= 80");
        assertTrue(javaScore.getBreakdown().contains("OOP:"));
        assertTrue(javaScore.getBreakdown().contains("Collections:"));
        assertTrue(javaScore.getStrengths().contains("Robust OOP architecture"));
    }

    @Test
    @DisplayName("Evaluate Spring Boot skill with controllers and validation calculates proper score")
    void testScoreSpringBoot_withControllersAndValidation_calculatesCorrectScore() {
        sampleProject.setSkills(Set.of(springBootSkill));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));

        List<Evidence> springEvidence = List.of(
                new Evidence(sampleProject, springBootSkill, "CONTROLLERS", "RestControllers mapping HTTP requests", 6),
                new Evidence(sampleProject, springBootSkill, "REST_ENDPOINTS", "Full CRUD endpoints", 22),
                new Evidence(sampleProject, springBootSkill, "SERVICE_LAYER", "Spring @Service components", 6),
                new Evidence(sampleProject, springBootSkill, "JPA_REPOSITORIES", "Spring Data JPA repositories", 5),
                new Evidence(sampleProject, springBootSkill, "BEAN_VALIDATION", "Jakarta constraints @Valid", 1),
                new Evidence(sampleProject, springBootSkill, "REST_CONTROLLER_ADVICE", "Global exception interception", 1)
        );
        when(evidenceRepository.findByProjectIdAndSkillId(1L, springBootSkill.getId())).thenReturn(springEvidence);

        List<ProofScoreResponse> scores = proofScoringEngine.analyzeProject(1L);

        assertEquals(1, scores.size());
        ProofScoreResponse springScore = scores.get(0);
        assertTrue(springScore.getScore() >= 85, "Full Spring Boot stack evidence should score >= 85");
        assertTrue(springScore.getBreakdown().contains("Controllers:"));
        assertTrue(springScore.getBreakdown().contains("Validation:"));
    }

    @Test
    @DisplayName("Evaluate SQL skill with normalized tables and ACID transactions awards relational points")
    void testScoreSql_withRelationalTablesAndTransactions_awardsRelationalPoints() {
        sampleProject.setSkills(Set.of(sqlSkill));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));

        List<Evidence> sqlEvidence = List.of(
                new Evidence(sampleProject, sqlSkill, "DATABASE_TABLES", "Normalized tables", 6),
                new Evidence(sampleProject, sqlSkill, "FOREIGN_KEY_RELATIONSHIPS", "Foreign key constraints", 5),
                new Evidence(sampleProject, sqlSkill, "CONSTRAINTS", "Unique indexes & not-null constraints", 4),
                new Evidence(sampleProject, sqlSkill, "ACID_TRANSACTIONS", "@Transactional write operations", 1)
        );
        when(evidenceRepository.findByProjectIdAndSkillId(1L, sqlSkill.getId())).thenReturn(sqlEvidence);

        List<ProofScoreResponse> scores = proofScoringEngine.analyzeProject(1L);

        assertEquals(1, scores.size());
        ProofScoreResponse score = scores.get(0);
        assertTrue(score.getScore() >= 60, "Basic relational SQL evidence should score >= 60");
        assertTrue(score.getBreakdown().contains("Tables:"));
        assertTrue(score.getBreakdown().contains("Transactions:"));
    }

    @Test
    @DisplayName("Evaluate REST API skill with verbs and DTOs evaluates successfully")
    void testScoreRestApi_withEndpointsAndDtos_evaluatesCorrectly() {
        sampleProject.setSkills(Set.of(restApiSkill));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));

        List<Evidence> restEvidence = List.of(
                new Evidence(sampleProject, restApiSkill, "HTTP_ENDPOINTS", "RESTful verbs GET, POST, PUT, DELETE", 22),
                new Evidence(sampleProject, restApiSkill, "STATUS_CODES", "Semantic HTTP response codes", 1),
                new Evidence(sampleProject, restApiSkill, "DTO_PATTERN", "Request/Response DTO separation", 1)
        );
        when(evidenceRepository.findByProjectIdAndSkillId(1L, restApiSkill.getId())).thenReturn(restEvidence);

        List<ProofScoreResponse> scores = proofScoringEngine.analyzeProject(1L);

        assertEquals(1, scores.size());
        ProofScoreResponse score = scores.get(0);
        assertTrue(score.getScore() >= 60);
        assertTrue(score.getStrengths().contains("RESTful URI design"));
    }

    @Test
    @DisplayName("Evaluate Testing skill calculates score based on test suite and assertions")
    void testScoreTesting_withUnitAndMockTests_calculatesCoverage() {
        sampleProject.setSkills(Set.of(testingSkill));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));

        List<Evidence> testEvidence = List.of(
                new Evidence(sampleProject, testingSkill, "UNIT_TESTS", "JUnit 5 test suite", 15),
                new Evidence(sampleProject, testingSkill, "MOCKITO_MOCKING", "Mockito dependency isolation", 1),
                new Evidence(sampleProject, testingSkill, "ASSERTIONS", "Boundary condition assertions", 1)
        );
        when(evidenceRepository.findByProjectIdAndSkillId(1L, testingSkill.getId())).thenReturn(testEvidence);

        List<ProofScoreResponse> scores = proofScoringEngine.analyzeProject(1L);

        assertEquals(1, scores.size());
        ProofScoreResponse score = scores.get(0);
        assertTrue(score.getScore() >= 70);
        assertTrue(score.getStrengths().contains("automated unit tests"));
    }

    @Test
    @DisplayName("Evaluate generic skill with empty evidence returns baseline score and recommendations")
    void testScoreGeneric_withEmptyEvidence_returnsBaselineScore() {
        sampleProject.setSkills(Set.of(genericSkill));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
        when(evidenceRepository.findByProjectIdAndSkillId(1L, genericSkill.getId())).thenReturn(Collections.emptyList());

        List<ProofScoreResponse> scores = proofScoringEngine.analyzeProject(1L);

        assertEquals(1, scores.size());
        ProofScoreResponse score = scores.get(0);
        assertEquals(25, score.getScore(), "Empty evidence should return baseline floor score 25");
        assertTrue(score.getRecommendations().contains("Register additional quantitative metrics"));
    }

    @Test
    @DisplayName("Score calculation never exceeds upper boundary of 100")
    void testScoreBoundary_neverExceeds100() {
        sampleProject.setSkills(Set.of(javaSkill));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));

        // Excessively high metrics
        List<Evidence> excessiveEvidence = List.of(
                new Evidence(sampleProject, javaSkill, "OOP_CONCEPTS", "OOP", 100),
                new Evidence(sampleProject, javaSkill, "COLLECTIONS_FRAMEWORK", "Collections", 500),
                new Evidence(sampleProject, javaSkill, "EXCEPTION_HANDLING", "Exceptions", 50),
                new Evidence(sampleProject, javaSkill, "BUSINESS_LOGIC_MODULES", "Services", 200),
                new Evidence(sampleProject, javaSkill, "UNIT_TESTS", "Tests", 1000)
        );
        when(evidenceRepository.findByProjectIdAndSkillId(1L, javaSkill.getId())).thenReturn(excessiveEvidence);

        List<ProofScoreResponse> scores = proofScoringEngine.analyzeProject(1L);

        assertEquals(1, scores.size());
        assertTrue(scores.get(0).getScore() <= 100, "Score must never exceed 100");
    }
}
