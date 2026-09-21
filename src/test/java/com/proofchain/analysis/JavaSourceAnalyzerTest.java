package com.proofchain.analysis;

import com.proofchain.analysis.JavaSourceAnalyzer.JavaAnalysisData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class JavaSourceAnalyzerTest {

    private JavaSourceAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new JavaSourceAnalyzer();
    }

    @Test
    void testJavaClassAndInterfaceDetection(@TempDir Path tempDir) throws IOException {
        Path pkg = tempDir.resolve("com/example");
        Files.createDirectories(pkg);

        String serviceCode = """
                package com.example;
                import java.util.List;
                import java.util.ArrayList;

                public interface UserService {
                    void process();
                }
                """;
        Files.writeString(pkg.resolve("UserService.java"), serviceCode);

        String implCode = """
                package com.example;
                import java.util.List;
                import java.util.ArrayList;

                public class UserServiceImpl implements UserService {
                    private List<String> users = new ArrayList<>();

                    public UserServiceImpl() {}

                    @Override
                    public void process() {
                        users.stream().filter(u -> u.length() > 2).forEach(System.out::println);
                    }
                }
                """;
        Files.writeString(pkg.resolve("UserServiceImpl.java"), implCode);

        JavaAnalysisData result = analyzer.analyze(tempDir);

        assertEquals(2, result.stats().getJavaFiles());
        assertEquals(1, result.stats().getClassCount());
        assertEquals(1, result.stats().getInterfaceCount());
        assertEquals(2, result.stats().getMethodCount());
        assertEquals(1, result.stats().getConstructorCount());

        // Check metrics generated
        assertTrue(result.metrics().stream().anyMatch(m -> m.getEvidenceType().equals("JAVA_CLASSES")));
        assertTrue(result.metrics().stream().anyMatch(m -> m.getEvidenceType().equals("OOP_CONCEPTS")));
        assertTrue(result.metrics().stream().anyMatch(m -> m.getEvidenceType().equals("COLLECTIONS_FRAMEWORK")));
    }

    @Test
    void testExceptionHandlingDetection(@TempDir Path tempDir) throws IOException {
        Path pkg = tempDir.resolve("com/example");
        Files.createDirectories(pkg);

        String exceptionCode = """
                package com.example;

                public class CustomOrderException extends RuntimeException {
                    public CustomOrderException(String msg) {
                        super(msg);
                    }
                }
                """;
        Files.writeString(pkg.resolve("CustomOrderException.java"), exceptionCode);

        String handlerCode = """
                package com.example;

                public class OrderHandler {
                    public void handle() {
                        try {
                            System.out.println("Processing");
                        } catch (Exception e) {
                            throw new CustomOrderException("Failed");
                        }
                    }
                }
                """;
        Files.writeString(pkg.resolve("OrderHandler.java"), handlerCode);

        JavaAnalysisData result = analyzer.analyze(tempDir);

        assertTrue(result.metrics().stream().anyMatch(m -> m.getEvidenceType().equals("EXCEPTION_HANDLING")));
    }

    @Test
    void testInvalidJavaSourceHandling(@TempDir Path tempDir) throws IOException {
        Path pkg = tempDir.resolve("com/example");
        Files.createDirectories(pkg);

        // Intentionally broken syntax
        String brokenCode = "public class BrokenClass { def invalid syntax here !!@@#";
        Files.writeString(pkg.resolve("BrokenClass.java"), brokenCode);

        String validCode = "package com.example; public class ValidClass {}";
        Files.writeString(pkg.resolve("ValidClass.java"), validCode);

        // Analyzer should not throw, should gracefully log and parse remaining files
        JavaAnalysisData result = analyzer.analyze(tempDir);

        assertEquals(2, result.stats().getJavaFiles());
        assertEquals(1, result.stats().getClassCount());
    }
}
