package com.thesismate.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thesismate.model.ReportVersion;

public interface ReportVersionRepository extends JpaRepository<ReportVersion, Long> {

    List<ReportVersion> findByReport_IdOrderByVersionNoDesc(Long reportId);

    Optional<ReportVersion> findFirstByReport_IdOrderByVersionNoDesc(Long reportId);
}
