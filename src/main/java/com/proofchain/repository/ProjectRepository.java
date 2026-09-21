package com.proofchain.repository;

import com.proofchain.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByUserId(Long userId);

    @Query("SELECT DISTINCT p FROM Project p JOIN p.skills s WHERE LOWER(s.name) = LOWER(:skillName)")
    List<Project> findProjectsBySkillName(@Param("skillName") String skillName);
}