package com.thesismate.dto;

import java.util.Set;

import com.thesismate.model.Track;

public record SupervisorCapacityView(
        Long profileId, String fullName, String bio, String officeRoom,
        Set<String> researchAreas, Set<Track> tracks,
        String semester, int capacity,
        long usedSlots, long remainingSlots, boolean available,
        Long version) {
}
