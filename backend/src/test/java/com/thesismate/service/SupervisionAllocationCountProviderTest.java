package com.thesismate.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.thesismate.model.Allocation;
import com.thesismate.repository.AllocationRepository;

@ExtendWith(MockitoExtension.class)
class SupervisionAllocationCountProviderTest {

    @Mock AllocationRepository allocations;

    @Test
    void countsOnlyActiveAllocationsInTheSemester() {
        when(allocations.countBySupervisorIdAndSemesterAndStatus(20L, "Spring 2027", Allocation.Status.ACTIVE))
                .thenReturn(2L);
        SupervisionAllocationCountProvider provider = new SupervisionAllocationCountProvider(allocations);
        assertEquals(2L, provider.countActiveAllocations(20L, "Spring 2027"));
    }
}