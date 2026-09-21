package com.proofchain.analysis;

import com.proofchain.dto.AnalysisReportResponse;
import com.proofchain.entity.Project;
import com.proofchain.entity.Skill;
import com.proofchain.entity.User;
import com.proofchain.repository.EvidenceRepository;
import com.proofchain.repository.ProjectRepository;
import com.proofchain.repository.ProofScoreRepository;
import com.proofchain.repository.SkillRepository;
import com.proofchain.repository.UserRepository;
import com.proofchain.service.EvidenceService;
import com.proofchain.service.ProofScoringEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectAnalysisServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SkillRepository skillRepository;
    @Mock
    private EvidenceRepository evidenceRepository;
    @Mock
    private ProofScoreRepository proofScoreRepository;
    @Mock
    private ProofScoringEngine proofScoringEngine;
    @Mock
    private EvidenceService evidenceService;

    private ProjectAnalysisService service;

    @BeforeEach
    void setUp() {
        ZipProjectExtractor extractor = new ZipProjectExtractor();
        GitHubProjectFetcher fetcher = new GitHubProjectFetcher(extractor);
        JavaSourceAnalyzer javaAnalyzer = new JavaSourceAnalyzer();
        SpringBootAnalyzer springAnalyzer = new SpringBootAnalyzer();
        TestAnalyzer testAnalyzer = new TestAnalyzer();
        BuildFileAnalyzer buildAnalyzer = new BuildFileAnalyzer();

        service = new ProjectAnalysisService(
                extractor, fetcher, javaAnalyzer, springAnalyzer, testAnalyzer, buildAnalyzer,
                projectRepository, userRepository, skillRepository, evidenceRepository,
                proofScoreRepository, proofScoringEngine, evidenceService
        );
    }

    @Test
    void testAnalyzeUploadedZipWithJavaSource() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // Add a sample App.java
            ZipEntry entry1 = new ZipEntry("src/main/java/com/example/App.java");
            zos.putNextEntry(entry1);
            zos.write("""
                    package com.example;
                    public class App {
                        public static void main(String[] args) {
                            System.out.println("Hello");
                        }
                    }
                    """.getBytes());
            zos.closeEntry();

            // Add a sample pom.xml
            ZipEntry entry2 = new ZipEntry("pom.xml");
            zos.putNextEntry(entry2);
            zos.write("""
                    <project>
                        <properties><java.version>21</java.version></properties>
                    </project>
                    """.getBytes());
            zos.closeEntry();
        }

        MockMultipartFile file = new MockMultipartFile(
                "file", "SampleApp.zip", "application/zip", baos.toByteArray()
        );

        User mockUser = new User("Test Dev", "test@proofchain.io", "pass");
        Skill javaSkill = new Skill("Java", "Core Java", "LANGUAGE");
        Project mockProject = new Project("SampleApp", "Description", null, mockUser);

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(projectRepository.findByUserId(mockUser.getId())).thenReturn(java.util.List.of());
        when(projectRepository.save(any(Project.class))).thenReturn(mockProject);
        when(skillRepository.findByNameIgnoreCase("Java")).thenReturn(Optional.of(javaSkill));
        when(proofScoringEngine.generateReport(any())).thenReturn(
                new com.proofchain.dto.ProjectReportResponse(
                        1L, "SampleApp", "Description", null, java.time.LocalDateTime.now(),
                        "Test Dev", 75, java.util.List.of(), java.util.Map.of(),
                        java.util.List.of(), java.util.List.of()
                )
        );

        AnalysisReportResponse response = service.analyzeUploadedZip(file, "SampleApp", "Description", null, 1L);

        assertNotNull(response);
        assertTrue(response.isJavaProject());
        assertEquals("SampleApp", response.getProjectName());
        assertEquals(1, response.getCodeStatistics().getJavaFiles());
        assertEquals(1, response.getCodeStatistics().getClassCount());
        assertEquals("Maven", response.getTechnologySummary().getBuildTool());

        verify(evidenceService).clearProjectEvidence(any());
        verify(evidenceRepository).saveAll(any());
        verify(proofScoringEngine).analyzeProject(any());
    }

    @Test
    void testAnalyzeNonJavaProjectZip() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry1 = new ZipEntry("script.py");
            zos.putNextEntry(entry1);
            zos.write("print('Hello Python')".getBytes());
            zos.closeEntry();
        }

        MockMultipartFile file = new MockMultipartFile(
                "file", "PythonProject.zip", "application/zip", baos.toByteArray()
        );

        User mockUser = new User("Test Dev", "test@proofchain.io", "pass");
        Project mockProject = new Project("PythonProject", "Python project", null, mockUser);

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(projectRepository.findByUserId(mockUser.getId())).thenReturn(java.util.List.of());
        when(projectRepository.save(any(Project.class))).thenReturn(mockProject);

        AnalysisReportResponse response = service.analyzeUploadedZip(file, "PythonProject", "Python project", null, 1L);

        assertNotNull(response);
        assertFalse(response.isJavaProject());
        assertEquals(0, response.getOverallProofScore());
        assertTrue(response.getStatusMessage().contains("No supported Java source files"));
    }
}
