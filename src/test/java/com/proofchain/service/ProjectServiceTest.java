package com.proofchain.service;

import com.proofchain.dto.ProjectRequest;
import com.proofchain.dto.ProjectResponse;
import com.proofchain.entity.Project;
import com.proofchain.entity.Skill;
import com.proofchain.entity.User;
import com.proofchain.exception.ResourceNotFoundException;
import com.proofchain.repository.EvidenceRepository;
import com.proofchain.repository.ProjectRepository;
import com.proofchain.repository.ProofScoreRepository;
import com.proofchain.repository.SkillRepository;
import com.proofchain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

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

    @InjectMocks
    private ProjectService projectService;

    private User sampleUser;
    private Project sampleProject;
    private Skill sampleSkill;

    @BeforeEach
    void setUp() {
        sampleUser = new User("Sathwik", "sathwik@proofchain.io", "hashedPass");
        sampleProject = new Project("ProofChain", "Verification platform", "https://github.com/sathwik/proofchain", sampleUser);
        sampleSkill = new Skill("Java", "Core language", "LANGUAGE");
    }

    @Test
    @DisplayName("Create project with valid user persists project and returns response")
    void createProject_withValidUser_createsProjectSuccessfully() {
        ProjectRequest request = new ProjectRequest(
                "ProofChain",
                "Verification platform",
                "https://github.com/sathwik/proofchain",
                1L,
                Set.of(10L)
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(skillRepository.findAllById(Set.of(10L))).thenReturn(List.of(sampleSkill));
        when(projectRepository.save(any(Project.class))).thenReturn(sampleProject);
        when(evidenceRepository.countByProjectId(any())).thenReturn(0L);
        when(proofScoreRepository.findByProjectId(any())).thenReturn(Collections.emptyList());

        ProjectResponse response = projectService.createProject(request);

        assertNotNull(response);
        assertEquals("ProofChain", response.getName());
        verify(projectRepository, times(1)).save(any(Project.class));
    }

    @Test
    @DisplayName("Create project with non-existent user throws ResourceNotFoundException")
    void createProject_whenUserNotFound_throwsResourceNotFoundException() {
        ProjectRequest request = new ProjectRequest("ProofChain", "Desc", "url", 99L, Set.of());
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> {
            projectService.createProject(request);
        });

        assertTrue(ex.getMessage().contains("User not found with id: 99"));
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    @DisplayName("Add skills to existing project associates skills and saves")
    void addSkills_toExistingProject_associatesSkills() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
        when(skillRepository.findAllById(Set.of(20L))).thenReturn(List.of(sampleSkill));
        when(projectRepository.save(any(Project.class))).thenReturn(sampleProject);
        when(evidenceRepository.countByProjectId(any())).thenReturn(0L);
        when(proofScoreRepository.findByProjectId(any())).thenReturn(Collections.emptyList());

        ProjectResponse response = projectService.addSkills(1L, Set.of(20L));

        assertNotNull(response);
        verify(projectRepository, times(1)).save(sampleProject);
    }
}
