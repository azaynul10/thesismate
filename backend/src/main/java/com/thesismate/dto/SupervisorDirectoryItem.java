package com.thesismate.dto;

import java.util.List;

import com.thesismate.model.Track;

/** F2 public supervisor card — never exposes passwordHash, email, studentId, or optimistic lock version. */
public record SupervisorDirectoryItem(
        Long profileId,
        Long supervisorId,
        String fullName,
        String bio,
        String officeRoom,
        List<String> researchAreas,
        List<Track> tracks,
        String semester,
        int capacity,
        long usedSlots,
        long remainingSlots,
        boolean available) {
}
