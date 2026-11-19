package com.thesismate.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thesismate.model.PlagiarismReport;

public interface PlagiarismReportRepository extends JpaRepository<PlagiarismReport, Long> {

    Optional<PlagiarismReport> findByVersion_Id(Long versionId);
}
