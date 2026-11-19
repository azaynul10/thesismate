package com.thesismate.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thesismate.model.Milestone;

public interface MilestoneRepository extends JpaRepository<Milestone, Long> {

    List<Milestone> findByGroup_IdOrderBySortOrderAscIdAsc(Long groupId);

    boolean existsByGroup_IdAndFromTemplateTrue(Long groupId);

    Optional<Milestone> findByGroup_IdAndTemplateKey(Long groupId, String templateKey);

    long countByGroup_Id(Long groupId);
}
