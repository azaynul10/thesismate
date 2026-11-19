package com.thesismate.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thesismate.model.SubmissionExtension;

public interface SubmissionExtensionRepository extends JpaRepository<SubmissionExtension, Long> {

    Optional<SubmissionExtension> findByGroup_IdAndWindow_Id(Long groupId, Long windowId);

    List<SubmissionExtension> findByWindow_Id(Long windowId);

    List<SubmissionExtension> findByGroup_Id(Long groupId);
}
