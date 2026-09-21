package com.proofchain.service;

import com.proofchain.dto.EvidenceRequest;
import com.proofchain.dto.EvidenceResponse;
import com.proofchain.entity.Evidence;
import com.proofchain.entity.Project;
import com.proofchain.entity.Skill;
import com.proofchain.entity.User;
import com.proofchain.exception.ResourceNotFoundException;
import com.proofchain.repository.EvidenceRepository;
import com.proofchain.repository.ProjectRepository;
import com.proofchain.repository.ProofScoreRepository;
import com.proofchain.repository.SkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvidenceServiceTest {

    @Mock
    private EvidenceRepository evidenceRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private ProofScoreRepository proofScoreRepository;

    @InjectMocks
    private EvidenceService evidenceService;

    private Project sampleProject;
    private Skill sampleSkill;

    @BeforeEach
    void setUp() {
        User user = new User("Sathwik", "sathwik@proofchain.io", "hashedPass");
        sampleProject = new Project("ProofChain", "Desc", "url", user);
        sampleProject.setSkills(new HashSet<>());
        sampleSkill = new Skill("Java", "Core language", "LANGUAGE");
    }

    @Test
    @DisplayName("Add evidence with valid project and skill saves evidence and updates project skills")
    void addEvidence_withValidProjectAndSkill_savesEvidence() {
        EvidenceRequest request = new EvidenceRequest(5L, "JAVA_CLASSES", "Domain classes", 35);
        Evidence savedEvidence = new Evidence(sampleProject, sampleSkill, "JAVA_CLASSES", "Domain classes", 35);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
        when(skillRepository.findById(5L)).thenReturn(Optional.of(sampleSkill));
        when(evidenceRepository.save(any(Evidence.class))).thenReturn(savedEvidence);

        EvidenceResponse response = evidenceService.addEvidence(1L, request);

        assertNotNull(response);
        assertEquals("JAVA_CLASSES", response.getEvidenceType());
        assertEquals(35, response.getNumericValue());
        verify(evidenceRepository, times(1)).save(any(Evidence.class));
    }

    @Test
    @DisplayName("Add evidence when project is missing throws ResourceNotFoundException")
    void addEvidence_whenProjectNotFound_throwsResourceNotFoundException() {
        EvidenceRequest request = new EvidenceRequest(5L, "JAVA_CLASSES", "Domain classes", 35);
        when(projectRepository.findById(404L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> {
            evidenceService.addEvidence(404L, request);
        });

        assertTrue(ex.getMessage().contains("Project not found with id: 404"));
        verify(evidenceRepository, never()).save(any(Evidence.class));
    }

    @Test
    @DisplayName("Clear project evidence removes all project evidence records and cached proof scores")
    void clearProjectEvidence_deletesAllProjectEvidenceAndScores() {
        when(projectRepository.existsById(1L)).thenReturn(true);
        List<Evidence> existingEvidence = List.of(
                new Evidence(sampleProject, sampleSkill, "OOP_CONCEPTS", "Desc", 4)
        );
        when(evidenceRepository.findByProjectId(1L)).thenReturn(existingEvidence);

        evidenceService.clearProjectEvidence(1L);

        verify(evidenceRepository, times(1)).deleteAll(existingEvidence);
        verify(proofScoreRepository, times(1)).deleteByProjectId(1L);
    }
}
