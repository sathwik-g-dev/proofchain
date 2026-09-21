package com.proofchain.analysis;

import com.proofchain.analysis.model.TechnologySummary;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Component
public class BuildFileAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(BuildFileAnalyzer.class);

    private static final Pattern MAVEN_JAVA_VERSION = Pattern.compile("<java\\.version>(.*?)</java\\.version>");
    private static final Pattern MAVEN_SPRING_BOOT_VERSION = Pattern.compile(
            "<artifactId>spring-boot-starter-parent</artifactId>\\s*<version>(.*?)</version>", Pattern.DOTALL
    );

    private static final Pattern GRADLE_JAVA_VERSION = Pattern.compile("(?:sourceCompatibility|targetCompatibility)\\s*=\\s*['\"]?(?:JavaVersion\\.VERSION_)?(\\d+)['\"]?");
    private static final Pattern GRADLE_SPRING_BOOT_VERSION = Pattern.compile("id\\s+['\"]org\\.springframework\\.boot['\"]\\s+version\\s+['\"](.*?)['\"]");

    public TechnologySummary analyze(Path rootDir) {
        TechnologySummary tech = new TechnologySummary();
        tech.setPrimaryLanguage("Java");

        Set<String> frameworks = new LinkedHashSet<>();
        Set<String> libraries = new LinkedHashSet<>();
        Set<String> databases = new LinkedHashSet<>();

        Path pomPath = findFile(rootDir, "pom.xml");
        Path gradlePath = findFile(rootDir, "build.gradle");
        if (gradlePath == null) {
            gradlePath = findFile(rootDir, "build.gradle.kts");
        }

        if (pomPath != null) {
            tech.setBuildTool("Maven");
            try {
                String content = Files.readString(pomPath);
                parseMavenPom(content, tech, frameworks, libraries, databases);
            } catch (Exception e) {
                log.warn("Error parsing pom.xml: {}", e.getMessage());
            }
        } else if (gradlePath != null) {
            tech.setBuildTool("Gradle");
            try {
                String content = Files.readString(gradlePath);
                parseGradleBuild(content, tech, frameworks, libraries, databases);
            } catch (Exception e) {
                log.warn("Error parsing build.gradle: {}", e.getMessage());
            }
        }

        tech.setFrameworks(new ArrayList<>(frameworks));
        tech.setLibraries(new ArrayList<>(libraries));
        tech.setDatabases(new ArrayList<>(databases));

        return tech;
    }

    private void parseMavenPom(String content, TechnologySummary tech, Set<String> frameworks, Set<String> libraries, Set<String> databases) {
        Matcher javaMatcher = MAVEN_JAVA_VERSION.matcher(content);
        if (javaMatcher.find()) {
            tech.setJavaVersion(javaMatcher.group(1).trim());
        }

        Matcher bootMatcher = MAVEN_SPRING_BOOT_VERSION.matcher(content);
        if (bootMatcher.find()) {
            frameworks.add("Spring Boot " + bootMatcher.group(1).trim());
        } else if (content.contains("spring-boot")) {
            frameworks.add("Spring Boot");
        }

        if (content.contains("spring-boot-starter-data-jpa") || content.contains("hibernate")) {
            frameworks.add("Spring Data JPA / Hibernate");
        }
        if (content.contains("spring-boot-starter-web")) {
            frameworks.add("Spring MVC / REST");
        }
        if (content.contains("spring-boot-starter-security") || content.contains("spring-security")) {
            frameworks.add("Spring Security");
        }

        // Databases
        if (content.contains("mysql-connector") || content.contains("mysql")) {
            databases.add("MySQL");
        }
        if (content.contains("postgresql")) {
            databases.add("PostgreSQL");
        }
        if (content.contains("h2")) {
            databases.add("H2 In-Memory DB");
        }

        // Libraries
        if (content.contains("spring-boot-starter-validation") || content.contains("hibernate-validator") || content.contains("validation-api")) {
            libraries.add("Jakarta / Bean Validation");
        }
        if (content.contains("junit") || content.contains("spring-boot-starter-test")) {
            libraries.add("JUnit 5");
        }
        if (content.contains("mockito")) {
            libraries.add("Mockito");
        }
        if (content.contains("lombok")) {
            libraries.add("Lombok");
        }
        if (content.contains("javaparser")) {
            libraries.add("JavaParser AST");
        }
    }

    private void parseGradleBuild(String content, TechnologySummary tech, Set<String> frameworks, Set<String> libraries, Set<String> databases) {
        Matcher javaMatcher = GRADLE_JAVA_VERSION.matcher(content);
        if (javaMatcher.find()) {
            tech.setJavaVersion(javaMatcher.group(1).trim());
        }

        Matcher bootMatcher = GRADLE_SPRING_BOOT_VERSION.matcher(content);
        if (bootMatcher.find()) {
            frameworks.add("Spring Boot " + bootMatcher.group(1).trim());
        } else if (content.contains("spring-boot")) {
            frameworks.add("Spring Boot");
        }

        if (content.contains("spring-boot-starter-data-jpa") || content.contains("hibernate")) {
            frameworks.add("Spring Data JPA / Hibernate");
        }
        if (content.contains("spring-boot-starter-web")) {
            frameworks.add("Spring MVC / REST");
        }

        if (content.contains("mysql")) {
            databases.add("MySQL");
        }
        if (content.contains("postgresql")) {
            databases.add("PostgreSQL");
        }
        if (content.contains("h2")) {
            databases.add("H2 In-Memory DB");
        }

        if (content.contains("junit")) {
            libraries.add("JUnit 5");
        }
        if (content.contains("mockito")) {
            libraries.add("Mockito");
        }
        if (content.contains("lombok")) {
            libraries.add("Lombok");
        }
    }

    private Path findFile(Path rootDir, String targetFileName) {
        try (Stream<Path> stream = Files.walk(rootDir, 3)) {
            return stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equalsIgnoreCase(targetFileName))
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }
}
