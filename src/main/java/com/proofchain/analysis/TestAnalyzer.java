package com.proofchain.analysis;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.proofchain.analysis.JavaSourceAnalyzer.CompilationUnitInfo;
import com.proofchain.analysis.model.CodeStatistics;
import com.proofchain.analysis.model.DetectedMetric;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class TestAnalyzer {

    private static final Set<String> TEST_ANNOTATIONS = Set.of(
            "Test", "ParameterizedTest", "RepeatedTest", "TestFactory"
    );

    private static final Set<String> MOCKITO_ANNOTATIONS = Set.of(
            "Mock", "InjectMocks", "Spy", "Captor", "MockBean", "SpyBean"
    );

    public record TestAnalysisData(
            int testFileCount,
            int testClassCount,
            int testMethodCount,
            List<DetectedMetric> metrics
    ) {}

    public TestAnalysisData analyze(List<CompilationUnitInfo> units, CodeStatistics stats) {
        List<DetectedMetric> metrics = new ArrayList<>();

        int testFiles = 0;
        int testClasses = 0;
        int testMethods = 0;
        int assertionCount = 0;
        int mockitoCount = 0;

        Set<String> testClassFiles = new LinkedHashSet<>();
        Set<String> mockitoFiles = new LinkedHashSet<>();
        Set<String> assertionFiles = new LinkedHashSet<>();

        for (CompilationUnitInfo info : units) {
            CompilationUnit cu = info.cu();
            String fileName = info.fileName();
            boolean isTestFile = info.isTestFile();

            boolean hasTestAnnotationInFile = false;

            for (ClassOrInterfaceDeclaration type : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                boolean classHasTests = false;

                for (MethodDeclaration method : type.getMethods()) {
                    for (AnnotationExpr ma : method.getAnnotations()) {
                        if (TEST_ANNOTATIONS.contains(ma.getNameAsString())) {
                            testMethods++;
                            classHasTests = true;
                            hasTestAnnotationInFile = true;
                        }
                    }
                }

                // Check Mockito annotations on fields
                for (FieldDeclaration field : type.getFields()) {
                    for (AnnotationExpr fa : field.getAnnotations()) {
                        if (MOCKITO_ANNOTATIONS.contains(fa.getNameAsString())) {
                            mockitoCount++;
                            mockitoFiles.add(fileName);
                        }
                    }
                }

                // Check Mockito method calls (Mockito.when, verify, mock)
                for (MethodCallExpr call : type.findAll(MethodCallExpr.class)) {
                    String callName = call.getNameAsString();
                    if ("when".equals(callName) || "verify".equals(callName) || "mock".equals(callName) || "doReturn".equals(callName)) {
                        mockitoCount++;
                        mockitoFiles.add(fileName);
                    } else if (callName.startsWith("assert") || "assertThat".equals(callName)) {
                        assertionCount++;
                        assertionFiles.add(fileName);
                    }
                }

                if (classHasTests || (isTestFile && !type.isInterface())) {
                    testClasses++;
                    testClassFiles.add(fileName);
                }
            }

            if (isTestFile || hasTestAnnotationInFile) {
                testFiles++;
            }
        }

        stats.setTestFiles(testFiles);
        stats.setTestClassCount(testClasses);
        stats.setTestMethodCount(testMethods);

        if (testMethods > 0 || testClasses > 0) {
            int testMetricVal = Math.max(testMethods, testClasses);

            metrics.add(new DetectedMetric(
                    "Unit & Integration Tests",
                    "TESTING",
                    "UNIT_TESTS",
                    testMetricVal,
                    String.format("Found %d test classes and %d executable @Test methods", testClasses, testMethods),
                    new ArrayList<>(testClassFiles)
            ));

            // Also map to Java testing metric
            metrics.add(new DetectedMetric(
                    "Automated Unit Tests",
                    "JAVA",
                    "UNIT_TESTS",
                    testMetricVal,
                    String.format("Found %d unit tests verifying behavior", testMetricVal),
                    new ArrayList<>(testClassFiles)
            ));
        }

        if (mockitoCount > 0) {
            metrics.add(new DetectedMetric(
                    "Mockito Dependency Mocking",
                    "TESTING",
                    "MOCKITO_MOCKING",
                    mockitoCount,
                    String.format("Found %d Mockito test mocks / verification calls", mockitoCount),
                    new ArrayList<>(mockitoFiles)
            ));
        }

        if (assertionCount > 0) {
            metrics.add(new DetectedMetric(
                    "Assertion Coverage",
                    "TESTING",
                    "ASSERTION_COVERAGE",
                    assertionCount,
                    String.format("Found %d assertions validating output conditions", assertionCount),
                    new ArrayList<>(assertionFiles)
            ));
        }

        return new TestAnalysisData(testFiles, testClasses, testMethods, metrics);
    }
}
