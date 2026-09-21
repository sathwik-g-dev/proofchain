package com.proofchain.service;

import com.proofchain.dto.SkillResponse;
import com.proofchain.entity.Skill;
import com.proofchain.exception.ResourceNotFoundException;
import com.proofchain.repository.SkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class SkillService {

    private final SkillRepository skillRepository;

    public SkillService(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    @Transactional(readOnly = true)
    public List<SkillResponse> getAllSkills() {
        return skillRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Skill getSkillEntity(Long id) {
        return skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public SkillResponse getSkillById(Long id) {
        return mapToResponse(getSkillEntity(id));
    }

    public Skill getOrCreateSkill(String name, String description, String category) {
        return skillRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> skillRepository.save(new Skill(name, description, category)));
    }

    private SkillResponse mapToResponse(Skill skill) {
        return new SkillResponse(
                skill.getId(),
                skill.getName(),
                skill.getDescription(),
                skill.getCategory()
        );
    }
}
