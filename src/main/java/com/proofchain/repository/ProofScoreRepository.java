package com.proofchain.repository;

import com.proofchain.entity.ProofScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProofScoreRepository extends JpaRepository<ProofScore, Long> {

    List<ProofScore> findByProjectId(Long projectId);

    Optional<ProofScore> findByProjectIdAndSkillId(Long projectId, Long skillId);

    List<ProofScore> findByProjectUserId(Long userId);

    void deleteByProjectId(Long projectId);
}
