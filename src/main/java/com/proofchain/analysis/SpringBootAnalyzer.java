package com.proofchain.analysis;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.proofchain.analysis.JavaSourceAnalyzer.CompilationUnitInfo;
import com.proofchain.analysis.model.CodeStatistics;
import com.proofchain.analysis.model.DetectedMetric;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class SpringBootAnalyzer {

    private static final Set<String> ENDPOINT_ANNOTATIONS = Set.of(
            "GetMapping", "PostMapping", "PutMapping", "DeleteMapping", "PatchMapping", "RequestMapping"
    );

    private static final Set<String> VALIDATION_ANNOTATIONS = Set.of(
            "Valid", "NotNull", "NotBlank", "NotEmpty", "Size", "Min", "Max", "Email", "Pattern", "Positive", "PositiveOrZero"
    );

    private static final Set<String> RELATIONSHIP_ANNOTATIONS = Set.of(
            "ManyToOne", "OneToMany", "ManyToMany", "OneToOne"
    );

    public record SpringAnalysisData(
            boolean isSpringBootProject,
            List<DetectedMetric> metrics
    ) {}

    public SpringAnalysisData analyze(List<CompilationUnitInfo> units, CodeStatistics stats) {
        List<DetectedMetric> metrics = new ArrayList<>();

        boolean hasSpringBootApplication = false;

        Set<String> controllerFiles = new LinkedHashSet<>();
        Set<String> endpointFiles = new LinkedHashSet<>();
        Set<String> serviceFiles = new LinkedHashSet<>();
        Set<String> repositoryFiles = new LinkedHashSet<>();
        Set<String> entityFiles = new LinkedHashSet<>();
        Set<String> validationFiles = new LinkedHashSet<>();
        Set<String> exceptionHandlerFiles = new LinkedHashSet<>();
        Set<String> dtoFiles = new LinkedHashSet<>();
        Set<String> transactionFiles = new LinkedHashSet<>();

        int controllerCount = 0;
        int endpointCount = 0;
        int serviceCount = 0;
        int repositoryCount = 0;
        int entityCount = 0;
        int validationCount = 0;
        int exceptionHandlerCount = 0;
        int dtoCount = 0;
        int transactionCount = 0;
        int relationshipCount = 0;
        int indexCount = 0;

        for (CompilationUnitInfo info : units) {
            if (info.isTestFile()) {
                continue;
            }

            CompilationUnit cu = info.cu();
            String fileName = info.fileName();

            // Check if DTO by naming pattern
            if (fileName.endsWith("Request.java") || fileName.endsWith("Response.java") ||
                    fileName.endsWith("DTO.java") || fileName.endsWith("Dto.java")) {
                dtoCount++;
                dtoFiles.add(fileName);
            }

            for (ClassOrInterfaceDeclaration type : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                boolean isController = false;
                boolean isService = false;
                boolean isRepository = false;
                boolean isEntity = false;
                boolean isAdvice = false;

                for (AnnotationExpr ann : type.getAnnotations()) {
                    String annName = ann.getNameAsString();

                    if ("SpringBootApplication".equals(annName)) {
                        hasSpringBootApplication = true;
                    } else if ("RestController".equals(annName) || "Controller".equals(annName)) {
                        isController = true;
                    } else if ("Service".equals(annName)) {
                        isService = true;
                    } else if ("Repository".equals(annName)) {
                        isRepository = true;
                    } else if ("Entity".equals(annName) || "Table".equals(annName)) {
                        isEntity = true;
                        if (ann.toString().contains("Index") || ann.toString().contains("indexes")) {
                            indexCount++;
                        }
                    } else if ("RestControllerAdvice".equals(annName) || "ControllerAdvice".equals(annName)) {
                        isAdvice = true;
                    } else if ("Transactional".equals(annName)) {
                        transactionCount++;
                        transactionFiles.add(fileName);
                    }
                }

                // Check Repository by extended interfaces
                for (var ext : type.getExtendedTypes()) {
                    String extName = ext.getNameAsString();
                    if (extName.contains("Repository") || extName.contains("JpaRepository") || extName.contains("CrudRepository")) {
                        isRepository = true;
                    }
                }

                if (isController) {
                    controllerCount++;
                    controllerFiles.add(fileName);
                }
                if (isService) {
                    serviceCount++;
                    serviceFiles.add(fileName);
                }
                if (isRepository) {
                    repositoryCount++;
                    repositoryFiles.add(fileName);
                }
                if (isEntity) {
                    entityCount++;
                    entityFiles.add(fileName);

                    // Scan fields for relationships
                    for (FieldDeclaration field : type.getFields()) {
                        for (AnnotationExpr fa : field.getAnnotations()) {
                            if (RELATIONSHIP_ANNOTATIONS.contains(fa.getNameAsString())) {
                                relationshipCount++;
                            }
                        }
                    }
                }
                if (isAdvice) {
                    exceptionHandlerCount++;
                    exceptionHandlerFiles.add(fileName);
                }

                // Scan methods for endpoints, transactions, validations, exception handlers
                for (MethodDeclaration method : type.getMethods()) {
                    for (AnnotationExpr ma : method.getAnnotations()) {
                        String mAnnName = ma.getNameAsString();

                        if (ENDPOINT_ANNOTATIONS.contains(mAnnName)) {
                            endpointCount++;
                            endpointFiles.add(fileName);
                        } else if ("Transactional".equals(mAnnName)) {
                            transactionCount++;
                            transactionFiles.add(fileName);
                        } else if ("ExceptionHandler".equals(mAnnName)) {
                            exceptionHandlerCount++;
                            exceptionHandlerFiles.add(fileName);
                        } else if (VALIDATION_ANNOTATIONS.contains(mAnnName)) {
                            validationCount++;
                            validationFiles.add(fileName);
                        }
                    }

                    // Method parameter validations
                    method.getParameters().forEach(param -> {
                        for (AnnotationExpr pa : param.getAnnotations()) {
                            if (VALIDATION_ANNOTATIONS.contains(pa.getNameAsString())) {
                                validationFiles.add(fileName);
                            }
                        }
                    });
                }

                // Field validations
                for (FieldDeclaration field : type.getFields()) {
                    for (AnnotationExpr fa : field.getAnnotations()) {
                        if (VALIDATION_ANNOTATIONS.contains(fa.getNameAsString())) {
                            validationCount++;
                            validationFiles.add(fileName);
                        }
                    }
                }
            }
        }

        // Update statistics
        stats.setControllerCount(controllerCount);
        stats.setEndpointCount(endpointCount);
        stats.setServiceCount(serviceCount);
        stats.setRepositoryCount(repositoryCount);
        stats.setEntityCount(entityCount);

        boolean isSpringProject = hasSpringBootApplication || controllerCount > 0 || serviceCount > 0 || repositoryCount > 0;

        // Metrics for Spring Boot
        if (controllerCount > 0) {
            metrics.add(new DetectedMetric(
                    "REST Controllers",
                    "SPRING_BOOT",
                    "CONTROLLERS",
                    controllerCount,
                    String.format("Found %d Spring @RestController / @Controller classes", controllerCount),
                    new ArrayList<>(controllerFiles)
            ));
        }

        if (endpointCount > 0) {
            metrics.add(new DetectedMetric(
                    "REST Endpoints",
                    "SPRING_BOOT",
                    "REST_ENDPOINTS",
                    endpointCount,
                    String.format("Detected %d mapped HTTP endpoints (GET, POST, PUT, DELETE, PATCH)", endpointCount),
                    new ArrayList<>(endpointFiles)
            ));

            // Also map to REST API skill
            metrics.add(new DetectedMetric(
                    "HTTP Endpoints",
                    "REST_API",
                    "HTTP_ENDPOINTS",
                    endpointCount,
                    String.format("RESTful URI surface with %d endpoints", endpointCount),
                    new ArrayList<>(endpointFiles)
            ));
        }

        if (serviceCount > 0) {
            metrics.add(new DetectedMetric(
                    "Service Layer",
                    "SPRING_BOOT",
                    "SERVICE_LAYER",
                    serviceCount,
                    String.format("Found %d Spring @Service business logic components", serviceCount),
                    new ArrayList<>(serviceFiles)
            ));

            // Also map to Java business logic module
            metrics.add(new DetectedMetric(
                    "Business Logic Services",
                    "JAVA",
                    "BUSINESS_LOGIC_MODULES",
                    serviceCount,
                    String.format("Found %d domain service classes", serviceCount),
                    new ArrayList<>(serviceFiles)
            ));
        }

        if (repositoryCount > 0) {
            metrics.add(new DetectedMetric(
                    "JPA Repositories",
                    "SPRING_BOOT",
                    "JPA_REPOSITORIES",
                    repositoryCount,
                    String.format("Found %d Spring Data JPA repository interfaces", repositoryCount),
                    new ArrayList<>(repositoryFiles)
            ));
        }

        if (validationCount > 0 || !validationFiles.isEmpty()) {
            metrics.add(new DetectedMetric(
                    "Bean Validation",
                    "SPRING_BOOT",
                    "BEAN_VALIDATION",
                    Math.max(1, validationCount),
                    String.format("Detected %d Jakarta / Bean Validation constraints (@Valid, @NotNull, etc.)", Math.max(1, validationCount)),
                    new ArrayList<>(validationFiles)
            ));
        }

        if (exceptionHandlerCount > 0) {
            metrics.add(new DetectedMetric(
                    "Global Exception Handler",
                    "SPRING_BOOT",
                    "REST_CONTROLLER_ADVICE",
                    exceptionHandlerCount,
                    "Detected centralized error translation (@RestControllerAdvice / @ExceptionHandler)",
                    new ArrayList<>(exceptionHandlerFiles)
            ));
        }

        // Metrics for SQL / Database
        if (entityCount > 0) {
            metrics.add(new DetectedMetric(
                    "Database Tables & Entities",
                    "SQL",
                    "DATABASE_TABLES",
                    entityCount,
                    String.format("Found %d JPA @Entity / @Table database entity mappings", entityCount),
                    new ArrayList<>(entityFiles)
            ));
        }

        if (relationshipCount > 0) {
            metrics.add(new DetectedMetric(
                    "Relational Associations",
                    "SQL",
                    "FOREIGN_KEY_RELATIONSHIPS",
                    relationshipCount,
                    String.format("Detected %d relational mappings (@ManyToOne, @OneToMany, @ManyToMany)", relationshipCount),
                    new ArrayList<>(entityFiles)
            ));
        }

        if (transactionCount > 0) {
            metrics.add(new DetectedMetric(
                    "Transactional Boundaries",
                    "SQL",
                    "ACID_TRANSACTIONS",
                    transactionCount,
                    String.format("Found %d @Transactional consistency boundaries", transactionCount),
                    new ArrayList<>(transactionFiles)
            ));
        }

        if (indexCount > 0) {
            metrics.add(new DetectedMetric(
                    "Database Indexes",
                    "SQL",
                    "INDEXES",
                    indexCount,
                    String.format("Detected %d explicit database indexes in entity annotations", indexCount),
                    new ArrayList<>(entityFiles)
            ));
        }

        // Metrics for REST API (DTOs and semantic status codes)
        if (dtoCount > 0) {
            metrics.add(new DetectedMetric(
                    "DTO Pattern",
                    "REST_API",
                    "DTO_PATTERN",
                    dtoCount,
                    String.format("Detected %d Request/Response DTO classes enforcing domain boundaries", dtoCount),
                    new ArrayList<>(dtoFiles)
            ));
        }

        if (endpointCount > 0) {
            metrics.add(new DetectedMetric(
                    "Semantic Status Codes",
                    "REST_API",
                    "STATUS_CODES",
                    1,
                    "REST controllers return semantic HTTP status codes via ResponseEntity",
                    new ArrayList<>(endpointFiles)
            ));
        }

        return new SpringAnalysisData(isSpringProject, metrics);
    }
}
