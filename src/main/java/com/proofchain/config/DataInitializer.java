package com.proofchain.config;

import com.proofchain.dto.EvidenceRequest;
import com.proofchain.dto.ProjectRequest;
import com.proofchain.dto.ProjectResponse;
import com.proofchain.entity.Skill;
import com.proofchain.entity.User;
import com.proofchain.repository.SkillRepository;
import com.proofchain.repository.UserRepository;
import com.proofchain.service.EvidenceService;
import com.proofchain.service.ProjectService;
import com.proofchain.service.ProofScoringEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;
import java.util.Set;

@Configuration
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final ProjectService projectService;
    private final EvidenceService evidenceService;
    private final ProofScoringEngine proofScoringEngine;

    public DataInitializer(UserRepository userRepository,
                           SkillRepository skillRepository,
                           ProjectService projectService,
                           EvidenceService evidenceService,
                           ProofScoringEngine proofScoringEngine) {
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
        this.projectService = projectService;
        this.evidenceService = evidenceService;
        this.proofScoringEngine = proofScoringEngine;
    }

    @Override
    public void run(String... args) {
        log.info("Initializing ProofChain skill catalog and demonstration data...");

        Skill java = getOrCreateSkill("Java", "Core language, OOP, Collections, Concurrency & Stream API", "LANGUAGE");
        Skill springBoot = getOrCreateSkill("Spring Boot", "Enterprise MVC, REST controllers, JPA & Dependency Injection", "FRAMEWORK");
        Skill sql = getOrCreateSkill("SQL", "Relational database schema, normalization, JOINs & transactions", "DATABASE");
        Skill restApi = getOrCreateSkill("REST API", "HTTP methods, status codes, DTOs & endpoint contract design", "ARCHITECTURE");
        Skill testing = getOrCreateSkill("Testing", "Automated unit tests, Mockito isolation & assertion coverage", "QUALITY");
        Skill git = getOrCreateSkill("Git", "Branching workflows, atomic commits & repository management", "TOOLING");

        // Seed or retrieve demo developer
        org.springframework.security.crypto.password.PasswordEncoder encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        User dev = userRepository.findByEmail("sathwik.dev@proofchain.io").orElseGet(() -> {
            User newUser = new User("Sathwik", "sathwik.dev@proofchain.io", encoder.encode("securePassword123"));
            return userRepository.save(newUser);
        });

        // If developer has no projects yet, seed ProofChain demonstration project
        if (projectService.getProjectsByUserId(dev.getId()).isEmpty()) {
            log.info("Seeding demonstration project 'ProofChain' with verified technical evidence...");

            ProjectRequest projectReq = new ProjectRequest(
                    "ProofChain",
                    "Evidence-based developer skill verification platform with a deterministic rule-based proof scoring engine and single-page workbench.",
                    "https://github.com/sathwik/proofchain",
                    dev.getId(),
                    Set.of(java.getId(), springBoot.getId(), sql.getId(), restApi.getId(), testing.getId())
            );
            ProjectResponse project = projectService.createProject(projectReq);
            Long pId = project.getId();

            // Java evidence (truthful metrics from codebase)
            evidenceService.addEvidence(pId, new EvidenceRequest(java.getId(), "JAVA_CLASSES", "Entities, services, controllers, DTOs, and exception handlers", 39));
            evidenceService.addEvidence(pId, new EvidenceRequest(java.getId(), "OOP_CONCEPTS", "Encapsulation, service abstraction, domain polymorphism, exception inheritance", 4));
            evidenceService.addEvidence(pId, new EvidenceRequest(java.getId(), "COLLECTIONS_FRAMEWORK", "List, Set, Map, LinkedHashMap, and Stream API operations", 8));
            evidenceService.addEvidence(pId, new EvidenceRequest(java.getId(), "EXCEPTION_HANDLING", "Custom runtime exceptions with centralized @RestControllerAdvice translation", 2));
            evidenceService.addEvidence(pId, new EvidenceRequest(java.getId(), "BUSINESS_LOGIC_MODULES", "Proof scoring engine, project orchestration, evidence aggregation, dashboard analytics", 4));

            // Spring Boot evidence (truthful metrics from codebase)
            evidenceService.addEvidence(pId, new EvidenceRequest(springBoot.getId(), "CONTROLLERS", "RestControllers mapping HTTP requests (User, Project, Skill, Evidence, ProofEngine, Dashboard)", 6));
            evidenceService.addEvidence(pId, new EvidenceRequest(springBoot.getId(), "REST_ENDPOINTS", "Full CRUD, batch evidence ingestion, and analytical reporting endpoints", 22));
            evidenceService.addEvidence(pId, new EvidenceRequest(springBoot.getId(), "SERVICE_LAYER", "Spring @Service components with transactional boundaries", 6));
            evidenceService.addEvidence(pId, new EvidenceRequest(springBoot.getId(), "JPA_REPOSITORIES", "Spring Data JPA repositories for User, Project, Skill, Evidence, ProofScore", 5));
            evidenceService.addEvidence(pId, new EvidenceRequest(springBoot.getId(), "BEAN_VALIDATION", "Jakarta validation constraints (@Valid, @NotBlank, @Size, @Min, @Email)", 1));
            evidenceService.addEvidence(pId, new EvidenceRequest(springBoot.getId(), "REST_CONTROLLER_ADVICE", "Centralized global exception handling with structured JSON error responses", 1));

            // SQL evidence (truthful metrics from codebase)
            evidenceService.addEvidence(pId, new EvidenceRequest(sql.getId(), "DATABASE_TABLES", "Normalized relational tables in MySQL (users, projects, skills, project_skills, evidence, proof_scores)", 6));
            evidenceService.addEvidence(pId, new EvidenceRequest(sql.getId(), "FOREIGN_KEY_RELATIONSHIPS", "Enforced relational foreign keys between users, projects, skills, evidence, and scores", 5));
            evidenceService.addEvidence(pId, new EvidenceRequest(sql.getId(), "CONSTRAINTS", "Primary keys, unique email/skill constraints, and not-null column definitions", 4));
            evidenceService.addEvidence(pId, new EvidenceRequest(sql.getId(), "ACID_TRANSACTIONS", "Declarative transaction boundaries (@Transactional) for write consistency", 1));
            evidenceService.addEvidence(pId, new EvidenceRequest(sql.getId(), "INDEXES", "Composite B-Tree performance indexes on (project_id, skill_id) in evidence and proof_scores", 2));
            evidenceService.addEvidence(pId, new EvidenceRequest(sql.getId(), "JPQL_QUERIES", "Explicit JPQL aggregation (GROUP BY/SUM) and JOIN queries", 2));

            // REST API evidence (truthful metrics from codebase)
            evidenceService.addEvidence(pId, new EvidenceRequest(restApi.getId(), "HTTP_ENDPOINTS", "Proper RESTful verbs (GET, POST, PUT, DELETE)", 22));
            evidenceService.addEvidence(pId, new EvidenceRequest(restApi.getId(), "STATUS_CODES", "Semantic HTTP response codes (200 OK, 201 Created, 204 No Content, 400, 404, 409)", 1));
            evidenceService.addEvidence(pId, new EvidenceRequest(restApi.getId(), "DTO_PATTERN", "Request/Response DTO separation preventing entity mass assignment", 1));

            // Testing evidence (truthful metrics verified in test suite)
            evidenceService.addEvidence(pId, new EvidenceRequest(testing.getId(), "UNIT_TESTS", "Suite of 16 unit and Mockito tests across scoring engine and services", 16));
            evidenceService.addEvidence(pId, new EvidenceRequest(testing.getId(), "MOCKITO_MOCKING", "Mockito dependency isolation verifying service and repository boundaries", 3));
            evidenceService.addEvidence(pId, new EvidenceRequest(testing.getId(), "INTEGRATION_TESTS", "Spring Boot application context initialization test suite", 1));

            // Execute initial proof analysis
            proofScoringEngine.analyzeProject(pId);
            log.info("ProofChain demonstration project seeded and analyzed successfully!");
        }
    }

    private Skill getOrCreateSkill(String name, String description, String category) {
        return skillRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> skillRepository.save(new Skill(name, description, category)));
    }
}
