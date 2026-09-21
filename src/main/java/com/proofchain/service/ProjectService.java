package com.proofchain.service;

import com.proofchain.dto.ProjectRequest;
import com.proofchain.dto.ProjectResponse;
import com.proofchain.entity.Project;
import com.proofchain.entity.ProofScore;
import com.proofchain.entity.Skill;
import com.proofchain.entity.User;
import com.proofchain.exception.ResourceNotFoundException;
import com.proofchain.repository.EvidenceRepository;
import com.proofchain.repository.ProjectRepository;
import com.proofchain.repository.ProofScoreRepository;
import com.proofchain.repository.SkillRepository;
import com.proofchain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final EvidenceRepository evidenceRepository;
    private final ProofScoreRepository proofScoreRepository;

    public ProjectService(ProjectRepository projectRepository,
                          UserRepository userRepository,
                          SkillRepository skillRepository,
                          EvidenceRepository evidenceRepository,
                          ProofScoreRepository proofScoreRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
        this.evidenceRepository = evidenceRepository;
        this.proofScoreRepository = proofScoreRepository;
    }

    public ProjectResponse createProject(ProjectRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        Project project = new Project(
                request.getName(),
                request.getDescription(),
                request.getGithubUrl(),
                user
        );

        if (request.getSkillIds() != null && !request.getSkillIds().isEmpty()) {
            Set<Skill> skills = new HashSet<>(skillRepository.findAllById(request.getSkillIds()));
            project.setSkills(skills);
        }

        Project saved = projectRepository.save(project);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects() {
        return projectRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
        return mapToResponse(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjectsByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        return projectRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ProjectResponse addSkills(Long projectId, Set<Long> skillIds) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        Set<Skill> skills = new HashSet<>(skillRepository.findAllById(skillIds));
        project.getSkills().addAll(skills);

        return mapToResponse(projectRepository.save(project));
    }

    public void deleteProject(Long id) {
        if (!projectRepository.existsById(id)) {
            throw new ResourceNotFoundException("Project not found with id: " + id);
        }
        projectRepository.deleteById(id);
    }

    private ProjectResponse mapToResponse(Project project) {
        List<String> skillNames = project.getSkills()
                .stream()
                .map(Skill::getName)
                .sorted()
                .collect(Collectors.toList());

        int evidenceCount = (int) evidenceRepository.countByProjectId(project.getId());

        List<ProofScore> scores = proofScoreRepository.findByProjectId(project.getId());
        Integer overallScore = null;
        if (!scores.isEmpty()) {
            int sum = scores.stream().mapToInt(ProofScore::getScore).sum();
            overallScore = Math.round((float) sum / scores.size());
        }

        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getGithubUrl(),
                project.getCreatedAt(),
                project.getUser().getId(),
                project.getUser().getName(),
                skillNames,
                evidenceCount,
                overallScore
        );
    }
}
