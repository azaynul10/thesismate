package com.thesismate.service;

public interface AllocationCountProvider {
    // ei semester e supervisor er active allocation koyta (F7 er table asha porjonto)
    long countActiveAllocations(Long supervisorUserId, String semester);
}