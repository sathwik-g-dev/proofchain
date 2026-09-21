package com.proofchain.repository;

import com.proofchain.entity.Evidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvidenceRepository extends JpaRepository<Evidence, Long> {

    List<Evidence> findByProjectId(Long projectId);

    List<Evidence> findByProjectIdAndSkillId(Long projectId, Long skillId);

    long countByProjectId(Long projectId);

    @Query("SELECT e.evidenceType, COALESCE(SUM(e.numericValue), 0) FROM Evidence e WHERE e.project.id = :projectId GROUP BY e.evidenceType")
    List<Object[]> getEvidenceSummaryByProjectId(@Param("projectId") Long projectId);
}
