package com.proofchain.analysis;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.proofchain.analysis.model.CodeStatistics;
import com.proofchain.analysis.model.DetectedMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Component
public class JavaSourceAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(JavaSourceAnalyzer.class);

    private static final Set<String> COLLECTION_TYPES = Set.of(
            "List", "ArrayList", "LinkedList", "Set", "HashSet", "LinkedHashSet", "TreeSet",
            "Map", "HashMap", "LinkedHashMap", "TreeMap", "Queue", "Deque", "ArrayDeque", "PriorityQueue",
            "Collection", "Collections", "Arrays"
    );

    private static final Set<String> STREAM_METHODS = Set.of(
            "stream", "parallelStream", "filter", "map", "flatMap", "collect", "reduce",
            "forEach", "distinct", "sorted", "anyMatch", "allMatch", "noneMatch", "findFirst", "findAny"
    );

    public record JavaAnalysisData(
            List<CompilationUnitInfo> units,
            CodeStatistics stats,
            List<DetectedMetric> metrics
    ) {}

    public record CompilationUnitInfo(
            Path path,
            String fileName,
            String relativePath,
            CompilationUnit cu,
            boolean isTestFile
    ) {}

    /**
     * Parses and analyzes all Java files in the root directory.
     */
    public JavaAnalysisData analyze(Path rootDir) throws IOException {
        List<CompilationUnitInfo> units = new ArrayList<>();
        CodeStatistics stats = new CodeStatistics();
        List<DetectedMetric> metrics = new ArrayList<>();

        List<Path> javaFiles;
        try (Stream<Path> stream = Files.walk(rootDir)) {
            javaFiles = stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".java"))
                    .toList();
        }

        stats.setJavaFiles(javaFiles.size());

        if (javaFiles.isEmpty()) {
            return new JavaAnalysisData(units, stats, metrics);
        }

        int totalLoc = 0;
        int totalClasses = 0;
        int totalInterfaces = 0;
        int totalEnums = 0;
        int totalRecords = 0;
        int totalMethods = 0;
        int totalFields = 0;
        int totalConstructors = 0;

        int inheritanceCount = 0;
        int interfaceImplCount = 0;
        int collectionsUsageCount = 0;
        int streamUsageCount = 0;
        int tryCatchCount = 0;
        int customExceptionCount = 0;

        Set<String> classFiles = new LinkedHashSet<>();
        Set<String> interfaceFiles = new LinkedHashSet<>();
        Set<String> inheritanceFiles = new LinkedHashSet<>();
        Set<String> collectionFiles = new LinkedHashSet<>();
        Set<String> streamFiles = new LinkedHashSet<>();
        Set<String> exceptionFiles = new LinkedHashSet<>();

        for (Path file : javaFiles) {
            String fileName = file.getFileName().toString();
            String relativePath = rootDir.relativize(file).toString().replace('\\', '/');
            boolean isTest = relativePath.contains("src/test/") || fileName.endsWith("Test.java") || fileName.endsWith("Tests.java");

            try {
                List<String> lines = Files.readAllLines(file);
                totalLoc += lines.size();
            } catch (Exception ignored) {
            }

            CompilationUnit cu;
            try {
                cu = StaticJavaParser.parse(file);
            } catch (Exception e) {
                log.warn("Could not parse Java file {} via AST: {}", relativePath, e.getMessage());
                continue;
            }

            units.add(new CompilationUnitInfo(file, fileName, relativePath, cu, isTest));

            // Class and interface declarations
            List<ClassOrInterfaceDeclaration> classesAndInterfaces = cu.findAll(ClassOrInterfaceDeclaration.class);
            for (ClassOrInterfaceDeclaration decl : classesAndInterfaces) {
                if (decl.isInterface()) {
                    totalInterfaces++;
                    interfaceFiles.add(fileName);
                } else {
                    totalClasses++;
                    classFiles.add(fileName);

                    if (!decl.getExtendedTypes().isEmpty()) {
                        inheritanceCount += decl.getExtendedTypes().size();
                        inheritanceFiles.add(fileName);

                        // Check if extends Exception / RuntimeException
                        for (var ext : decl.getExtendedTypes()) {
                            String extName = ext.getNameAsString();
                            if (extName.contains("Exception") || extName.contains("Throwable") || extName.contains("Error")) {
                                customExceptionCount++;
                                exceptionFiles.add(fileName);
                            }
                        }
                    }

                    if (!decl.getImplementedTypes().isEmpty()) {
                        interfaceImplCount += decl.getImplementedTypes().size();
                        inheritanceFiles.add(fileName);
                    }
                }
            }

            // Enums & Records
            totalEnums += cu.findAll(EnumDeclaration.class).size();
            totalRecords += cu.findAll(RecordDeclaration.class).size();

            // Methods & Constructors & Fields
            totalMethods += cu.findAll(MethodDeclaration.class).size();
            totalConstructors += cu.findAll(ConstructorDeclaration.class).size();
            totalFields += cu.findAll(FieldDeclaration.class).size();

            // Collections Framework usage detection
            boolean fileHasCollections = false;
            for (FieldDeclaration field : cu.findAll(FieldDeclaration.class)) {
                String typeStr = field.getElementType().asString();
                if (isCollectionType(typeStr)) {
                    collectionsUsageCount++;
                    fileHasCollections = true;
                }
            }
            for (MethodDeclaration method : cu.findAll(MethodDeclaration.class)) {
                String retType = method.getType().asString();
                if (isCollectionType(retType)) {
                    collectionsUsageCount++;
                    fileHasCollections = true;
                }
            }
            if (fileHasCollections) {
                collectionFiles.add(fileName);
            }

            // Stream API usage detection
            List<MethodCallExpr> methodCalls = cu.findAll(MethodCallExpr.class);
            boolean fileHasStreams = false;
            for (MethodCallExpr call : methodCalls) {
                String callName = call.getNameAsString();
                if (STREAM_METHODS.contains(callName)) {
                    streamUsageCount++;
                    fileHasStreams = true;
                }
            }
            if (fileHasStreams) {
                streamFiles.add(fileName);
            }

            // Exception Handling (try/catch, throw)
            List<TryStmt> tryStmts = cu.findAll(TryStmt.class);
            if (!tryStmts.isEmpty()) {
                tryCatchCount += tryStmts.size();
                exceptionFiles.add(fileName);
            }
            List<ThrowStmt> throwStmts = cu.findAll(ThrowStmt.class);
            if (!throwStmts.isEmpty()) {
                exceptionFiles.add(fileName);
            }
        }

        stats.setTotalLinesOfCode(totalLoc);
        stats.setClassCount(totalClasses);
        stats.setInterfaceCount(totalInterfaces);
        stats.setEnumCount(totalEnums);
        stats.setRecordCount(totalRecords);
        stats.setMethodCount(totalMethods);
        stats.setFieldCount(totalFields);
        stats.setConstructorCount(totalConstructors);

        // Populate DetectedMetrics for Java
        metrics.add(new DetectedMetric(
                "Java Classes",
                "JAVA",
                "JAVA_CLASSES",
                totalClasses,
                String.format("Found %d concrete classes across source files", totalClasses),
                new ArrayList<>(classFiles)
        ));

        int oopTotal = inheritanceCount + interfaceImplCount;
        if (oopTotal > 0 || totalInterfaces > 0) {
            metrics.add(new DetectedMetric(
                    "OOP Architecture",
                    "JAVA",
                    "OOP_CONCEPTS",
                    Math.max(1, oopTotal),
                    String.format("Detected %d inheritance/interface implementations and %d interfaces", oopTotal, totalInterfaces),
                    new ArrayList<>(inheritanceFiles.isEmpty() ? interfaceFiles : inheritanceFiles)
            ));
        }

        int collectionsTotal = collectionsUsageCount + streamUsageCount;
        if (collectionsTotal > 0) {
            Set<String> combined = new LinkedHashSet<>(collectionFiles);
            combined.addAll(streamFiles);
            metrics.add(new DetectedMetric(
                    "Collections & Stream API",
                    "JAVA",
                    "COLLECTIONS_FRAMEWORK",
                    collectionsTotal,
                    String.format("Detected %d collection references and %d Stream API operations", collectionsUsageCount, streamUsageCount),
                    new ArrayList<>(combined)
            ));
        }

        int exceptionsTotal = tryCatchCount + customExceptionCount;
        if (exceptionsTotal > 0) {
            metrics.add(new DetectedMetric(
                    "Exception Handling",
                    "JAVA",
                    "EXCEPTION_HANDLING",
                    exceptionsTotal,
                    String.format("Detected %d try-catch blocks and %d custom exception classes", tryCatchCount, customExceptionCount),
                    new ArrayList<>(exceptionFiles)
            ));
        }

        if (!javaFiles.isEmpty()) {
            metrics.add(new DetectedMetric(
                    "Java Source Files",
                    "JAVA",
                    "JAVA_SOURCE_FILES",
                    javaFiles.size(),
                    String.format("Found %d Java source files analyzed", javaFiles.size()),
                    javaFiles.stream().map(p -> p.getFileName().toString()).distinct().limit(10).toList()
            ));
        }

        if (totalMethods > 0) {
            metrics.add(new DetectedMetric(
                    "Declared Methods",
                    "JAVA",
                    "DECLARED_METHODS",
                    totalMethods,
                    String.format("Detected %d declared methods across source files", totalMethods),
                    List.of()
            ));
        }

        return new JavaAnalysisData(units, stats, metrics);
    }

    private boolean isCollectionType(String typeStr) {
        for (String coll : COLLECTION_TYPES) {
            if (typeStr.contains(coll)) {
                return true;
            }
        }
        return false;
    }
}
