package com.proofchain.service;

import com.proofchain.dto.DashboardResponse;
import com.proofchain.dto.ProjectResponse;
import com.proofchain.dto.ProofScoreResponse;
import com.proofchain.entity.Project;
import com.proofchain.entity.ProofScore;
import com.proofchain.entity.User;
import com.proofchain.exception.ResourceNotFoundException;
import com.proofchain.repository.ProjectRepository;
import com.proofchain.repository.ProofScoreRepository;
import com.proofchain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProofScoreRepository proofScoreRepository;
    private final ProjectService projectService;

    public DashboardService(UserRepository userRepository,
                            ProjectRepository projectRepository,
                            ProofScoreRepository proofScoreRepository,
                            ProjectService projectService) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.proofScoreRepository = proofScoreRepository;
        this.projectService = projectService;
    }

    public DashboardResponse getDashboard(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Developer not found with id: " + userId));

        List<ProjectResponse> projects = projectService.getProjectsByUserId(userId);
        List<ProofScore> allScores = proofScoreRepository.findByProjectUserId(userId);

        Map<Long, List<ProofScore>> skillGroupMap = new HashMap<>();
        for (ProofScore ps : allScores) {
            skillGroupMap.computeIfAbsent(ps.getSkill().getId(), k -> new ArrayList<>()).add(ps);
        }

        List<ProofScoreResponse> skillProfiles = new ArrayList<>();
        int totalScoreSum = 0;

        for (Map.Entry<Long, List<ProofScore>> entry : skillGroupMap.entrySet()) {
            List<ProofScore> list = entry.getValue();
            int avgScore = Math.round((float) list.stream().mapToInt(ProofScore::getScore).sum() / list.size());
            totalScoreSum += avgScore;

            ProofScore latest = list.get(list.size() - 1);
            skillProfiles.add(new ProofScoreResponse(
                    latest.getId(),
                    latest.getSkill().getId(),
                    latest.getSkill().getName(),
                    avgScore,
                    latest.getBreakdown(),
                    latest.getStrengths(),
                    latest.getRecommendations(),
                    latest.getCalculatedAt()
            ));
        }

        Integer overallScore = null;
        if (!skillProfiles.isEmpty()) {
            overallScore = Math.round((float) totalScoreSum / skillProfiles.size());
        }

        return new DashboardResponse(
                user.getId(),
                user.getName(),
                overallScore,
                projects.size(),
                skillProfiles.size(),
                allScores.size(),
                skillProfiles,
                projects
        );
    }
}
