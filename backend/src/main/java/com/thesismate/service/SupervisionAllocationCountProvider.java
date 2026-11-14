package com.thesismate.service;

import org.springframework.stereotype.Component;

import com.thesismate.model.Allocation;
import com.thesismate.repository.AllocationRepository;

/** Used slots for a supervisor are the groups they are currently allocated to in that semester. */
@Component
public class SupervisionAllocationCountProvider implements AllocationCountProvider {

    private final AllocationRepository allocations;

    public SupervisionAllocationCountProvider(AllocationRepository allocations) {
        this.allocations = allocations;
    }

    @Override
    public long countActiveAllocations(Long supervisorUserId, String semester) {
        return allocations.countBySupervisorIdAndSemesterAndStatus(
                supervisorUserId, semester, Allocation.Status.ACTIVE);
    }
}