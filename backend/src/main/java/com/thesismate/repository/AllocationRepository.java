package com.thesismate.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thesismate.model.Allocation;
import com.thesismate.model.SupervisorRole;

public interface AllocationRepository extends JpaRepository<Allocation, Long> {
    long countBySupervisorIdAndSemesterAndStatus(Long supervisorId, String semester, Allocation.Status status);
    long countByGroupIdAndSupervisorRoleAndStatus(Long groupId, SupervisorRole role, Allocation.Status status);
    boolean existsByGroupIdAndSupervisorIdAndStatus(Long groupId, Long supervisorId, Allocation.Status status);
    List<Allocation> findByGroupIdAndStatus(Long groupId, Allocation.Status status);
    List<Allocation> findBySupervisorIdAndStatus(Long supervisorId, Allocation.Status status);
}