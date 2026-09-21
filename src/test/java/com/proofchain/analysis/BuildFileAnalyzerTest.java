package com.proofchain.analysis;

import com.proofchain.analysis.model.TechnologySummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class BuildFileAnalyzerTest {

    private BuildFileAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new BuildFileAnalyzer();
    }

    @Test
    void testMavenPomAnalysis(@TempDir Path tempDir) throws IOException {
        String pomContent = """
                <project>
                    <parent>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-parent</artifactId>
                        <version>3.3.3</version>
                    </parent>
                    <properties>
                        <java.version>21</java.version>
                    </properties>
                    <dependencies>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-web</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-data-jpa</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>com.mysql</groupId>
                            <artifactId>mysql-connector-j</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-test</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """;

        Files.writeString(tempDir.resolve("pom.xml"), pomContent);

        TechnologySummary tech = analyzer.analyze(tempDir);

        assertEquals("Maven", tech.getBuildTool());
        assertEquals("21", tech.getJavaVersion());
        assertTrue(tech.getFrameworks().stream().anyMatch(f -> f.contains("Spring Boot 3.3.3")));
        assertTrue(tech.getFrameworks().stream().anyMatch(f -> f.contains("Spring Data JPA")));
        assertTrue(tech.getDatabases().contains("MySQL"));
        assertTrue(tech.getLibraries().contains("JUnit 5"));
    }

    @Test
    void testGradleBuildAnalysis(@TempDir Path tempDir) throws IOException {
        String gradleContent = """
                plugins {
                    id 'org.springframework.boot' version '3.2.0'
                }
                sourceCompatibility = '17'
                dependencies {
                    implementation 'org.springframework.boot:spring-boot-starter-web'
                }
                """;

        Files.writeString(tempDir.resolve("build.gradle"), gradleContent);

        TechnologySummary tech = analyzer.analyze(tempDir);

        assertEquals("Gradle", tech.getBuildTool());
        assertEquals("17", tech.getJavaVersion());
        assertTrue(tech.getFrameworks().stream().anyMatch(f -> f.contains("Spring Boot 3.2.0")));
    }
}
