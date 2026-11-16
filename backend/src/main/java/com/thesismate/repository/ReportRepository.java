package com.thesismate.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thesismate.model.Phase;
import com.thesismate.model.Report;

public interface ReportRepository extends JpaRepository<Report, Long> {

    Optional<Report> findByGroup_IdAndPhase(Long groupId, Phase phase);

    List<Report> findByGroup_Id(Long groupId);
}
