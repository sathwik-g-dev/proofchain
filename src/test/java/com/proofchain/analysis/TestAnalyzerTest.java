package com.proofchain.analysis;

import com.github.javaparser.StaticJavaParser;
import com.proofchain.analysis.JavaSourceAnalyzer.CompilationUnitInfo;
import com.proofchain.analysis.TestAnalyzer.TestAnalysisData;
import com.proofchain.analysis.model.CodeStatistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestAnalyzerTest {

    private TestAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new TestAnalyzer();
    }

    @Test
    void testTestMethodsAndMockitoDetection() {
        String testClassCode = """
                package com.example;
                import org.junit.jupiter.api.Test;
                import org.mockito.Mock;
                import static org.junit.jupiter.api.Assertions.*;
                import static org.mockito.Mockito.*;

                public class UserServiceTest {

                    @Mock
                    private Object repo;

                    @Test
                    void testFind() {
                        when(repo.toString()).thenReturn("mocked");
                        assertEquals("mocked", repo.toString());
                        assertTrue(true);
                    }

                    @Test
                    void testSave() {
                        assertNotNull(repo);
                    }
                }
                """;

        List<CompilationUnitInfo> units = List.of(
                new CompilationUnitInfo(Path.of("UserServiceTest.java"), "UserServiceTest.java", "src/test/java/UserServiceTest.java", StaticJavaParser.parse(testClassCode), true)
        );

        CodeStatistics stats = new CodeStatistics();
        TestAnalysisData result = analyzer.analyze(units, stats);

        assertEquals(1, result.testClassCount());
        assertEquals(2, result.testMethodCount());
        assertEquals(1, result.testFileCount());

        assertTrue(result.metrics().stream().anyMatch(m -> m.getEvidenceType().equals("UNIT_TESTS")));
        assertTrue(result.metrics().stream().anyMatch(m -> m.getEvidenceType().equals("MOCKITO_MOCKING")));
        assertTrue(result.metrics().stream().anyMatch(m -> m.getEvidenceType().equals("ASSERTION_COVERAGE")));
    }
}
