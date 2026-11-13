package com.thesismate.service;

import org.springframework.stereotype.Component;

@Component
public class NoOpAllocationCountProvider implements AllocationCountProvider {

    @Override
    public long countActiveAllocations(Long supervisorUserId, String semester) {
        return 0L;
    }
}