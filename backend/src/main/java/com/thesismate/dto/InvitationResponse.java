package com.thesismate.dto;

import java.time.LocalDateTime;

import com.thesismate.model.Track;

public record InvitationResponse(
        Long id,
        Long groupId,
        Track track,
        String semester,
        String invitedByName,
        String status,
        LocalDateTime createdAt) {
}