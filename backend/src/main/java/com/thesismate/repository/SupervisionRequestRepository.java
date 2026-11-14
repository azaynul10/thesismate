package com.thesismate.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thesismate.model.SupervisionRequest;

public interface SupervisionRequestRepository extends JpaRepository<SupervisionRequest, Long> {
    long countByGroupIdAndStatus(Long groupId, SupervisionRequest.Status status);
    boolean existsByGroupIdAndSupervisorIdAndStatus(Long groupId, Long supervisorId, SupervisionRequest.Status status);
    List<SupervisionRequest> findByGroupIdOrderByCreatedAtDesc(Long groupId);
    List<SupervisionRequest> findByGroupIdAndStatus(Long groupId, SupervisionRequest.Status status);
    List<SupervisionRequest> findBySupervisorIdAndStatusOrderByCreatedAtAsc(Long supervisorId, SupervisionRequest.Status status);
    List<SupervisionRequest> findByStatusAndExpiresAtBefore(SupervisionRequest.Status status, LocalDateTime time);
}