package com.thesismate.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.thesismate.model.SupervisorRole;
import com.thesismate.model.Track;

public record SupervisionRequestResponse(
        Long id,
        Long groupId,
        Track track,
        String semester,
        Long supervisorId,
        String supervisorName,
        SupervisorRole role,
        String pitch,
        String status,
        String rejectionReason,
        List<String> groupMembers,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        LocalDateTime respondedAt) {
}