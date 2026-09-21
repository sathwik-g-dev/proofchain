package com.proofchain.analysis;

import com.github.javaparser.StaticJavaParser;
import com.proofchain.analysis.JavaSourceAnalyzer.CompilationUnitInfo;
import com.proofchain.analysis.SpringBootAnalyzer.SpringAnalysisData;
import com.proofchain.analysis.model.CodeStatistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SpringBootAnalyzerTest {

    private SpringBootAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new SpringBootAnalyzer();
    }

    @Test
    void testSpringComponentsAndEndpointDetection() {
        String controllerCode = """
                package com.example.controller;
                import org.springframework.web.bind.annotation.*;
                import jakarta.validation.Valid;

                @RestController
                @RequestMapping("/api/users")
                public class UserController {

                    @GetMapping
                    public String getAll() { return "users"; }

                    @PostMapping
                    public String create(@Valid String name) { return "created"; }

                    @DeleteMapping("/{id}")
                    public void delete(@PathVariable Long id) {}
                }
                """;

        String serviceCode = """
                package com.example.service;
                import org.springframework.stereotype.Service;
                import org.springframework.transaction.annotation.Transactional;

                @Service
                public class UserService {
                    @Transactional
                    public void doWork() {}
                }
                """;

        String entityCode = """
                package com.example.entity;
                import jakarta.persistence.*;

                @Entity
                @Table(name = "users")
                public class User {
                    @Id
                    private Long id;

                    @ManyToOne
                    private Object role;
                }
                """;

        String repoCode = """
                package com.example.repository;
                import org.springframework.data.jpa.repository.JpaRepository;
                import org.springframework.stereotype.Repository;

                @Repository
                public interface UserRepository extends JpaRepository<Object, Long> {}
                """;

        List<CompilationUnitInfo> units = List.of(
                new CompilationUnitInfo(Path.of("UserController.java"), "UserController.java", "src/main/java/UserController.java", StaticJavaParser.parse(controllerCode), false),
                new CompilationUnitInfo(Path.of("UserService.java"), "UserService.java", "src/main/java/UserService.java", StaticJavaParser.parse(serviceCode), false),
                new CompilationUnitInfo(Path.of("User.java"), "User.java", "src/main/java/User.java", StaticJavaParser.parse(entityCode), false),
                new CompilationUnitInfo(Path.of("UserRepository.java"), "UserRepository.java", "src/main/java/UserRepository.java", StaticJavaParser.parse(repoCode), false)
        );

        CodeStatistics stats = new CodeStatistics();
        SpringAnalysisData result = analyzer.analyze(units, stats);

        assertTrue(result.isSpringBootProject());
        assertEquals(1, stats.getControllerCount());
        assertEquals(3, stats.getEndpointCount());
        assertEquals(1, stats.getServiceCount());
        assertEquals(1, stats.getRepositoryCount());
        assertEquals(1, stats.getEntityCount());

        // Check metrics
        assertTrue(result.metrics().stream().anyMatch(m -> m.getEvidenceType().equals("CONTROLLERS")));
        assertTrue(result.metrics().stream().anyMatch(m -> m.getEvidenceType().equals("REST_ENDPOINTS")));
        assertTrue(result.metrics().stream().anyMatch(m -> m.getEvidenceType().equals("SERVICE_LAYER")));
        assertTrue(result.metrics().stream().anyMatch(m -> m.getEvidenceType().equals("JPA_REPOSITORIES")));
        assertTrue(result.metrics().stream().anyMatch(m -> m.getEvidenceType().equals("DATABASE_TABLES")));
        assertTrue(result.metrics().stream().anyMatch(m -> m.getEvidenceType().equals("FOREIGN_KEY_RELATIONSHIPS")));
        assertTrue(result.metrics().stream().anyMatch(m -> m.getEvidenceType().equals("ACID_TRANSACTIONS")));
    }
}
