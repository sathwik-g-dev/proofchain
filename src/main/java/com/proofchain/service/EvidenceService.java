package com.proofchain.service;

import com.proofchain.dto.EvidenceRequest;
import com.proofchain.dto.EvidenceResponse;
import com.proofchain.entity.Evidence;
import com.proofchain.entity.Project;
import com.proofchain.entity.Skill;
import com.proofchain.exception.ResourceNotFoundException;
import com.proofchain.repository.EvidenceRepository;
import com.proofchain.repository.ProjectRepository;
import com.proofchain.repository.ProofScoreRepository;
import com.proofchain.repository.SkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class EvidenceService {

    private final EvidenceRepository evidenceRepository;
    private final ProjectRepository projectRepository;
    private final SkillRepository skillRepository;
    private final ProofScoreRepository proofScoreRepository;

    public EvidenceService(EvidenceRepository evidenceRepository,
                           ProjectRepository projectRepository,
                           SkillRepository skillRepository,
                           ProofScoreRepository proofScoreRepository) {
        this.evidenceRepository = evidenceRepository;
        this.projectRepository = projectRepository;
        this.skillRepository = skillRepository;
        this.proofScoreRepository = proofScoreRepository;
    }

    public EvidenceResponse addEvidence(Long projectId, EvidenceRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        Skill skill = skillRepository.findById(request.getSkillId())
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found with id: " + request.getSkillId()));

        // Also ensure skill is in project's skill list
        if (!project.getSkills().contains(skill)) {
            project.getSkills().add(skill);
            projectRepository.save(project);
        }

        Evidence evidence = new Evidence(
                project,
                skill,
                request.getEvidenceType().trim().toUpperCase(),
                request.getDescription(),
                request.getNumericValue() != null ? request.getNumericValue() : 1
        );

        Evidence saved = evidenceRepository.save(evidence);
        return mapToResponse(saved);
    }

    public List<EvidenceResponse> addEvidenceBatch(Long projectId, List<EvidenceRequest> requests) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        List<Evidence> toSave = new ArrayList<>();
        for (EvidenceRequest req : requests) {
            Skill skill = skillRepository.findById(req.getSkillId())
                    .orElseThrow(() -> new ResourceNotFoundException("Skill not found with id: " + req.getSkillId()));

            if (!project.getSkills().contains(skill)) {
                project.getSkills().add(skill);
            }

            toSave.add(new Evidence(
                    project,
                    skill,
                    req.getEvidenceType().trim().toUpperCase(),
                    req.getDescription(),
                    req.getNumericValue() != null ? req.getNumericValue() : 1
            ));
        }

        projectRepository.save(project);
        List<Evidence> saved = evidenceRepository.saveAll(toSave);
        return saved.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EvidenceResponse> getProjectEvidence(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found with id: " + projectId);
        }
        return evidenceRepository.findByProjectId(projectId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public void deleteEvidence(Long id) {
        if (!evidenceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Evidence not found with id: " + id);
        }
        evidenceRepository.deleteById(id);
    }

    public void clearProjectEvidence(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found with id: " + projectId);
        }
        List<Evidence> list = evidenceRepository.findByProjectId(projectId);
        evidenceRepository.deleteAll(list);
        proofScoreRepository.deleteByProjectId(projectId);
    }

    private EvidenceResponse mapToResponse(Evidence e) {
        return new EvidenceResponse(
                e.getId(),
                e.getProject().getId(),
                e.getProject().getName(),
                e.getSkill().getId(),
                e.getSkill().getName(),
                e.getEvidenceType(),
                e.getDescription(),
                e.getNumericValue(),
                e.getCreatedAt()
        );
    }
}
