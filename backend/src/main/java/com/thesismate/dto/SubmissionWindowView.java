package com.thesismate.dto;

import java.time.LocalDateTime;

import com.thesismate.model.LatePolicy;
import com.thesismate.model.Phase;
import com.thesismate.model.Track;

public record SubmissionWindowView(
        Long id, String semester, Phase phase, Track track,
        LocalDateTime opensAt, LocalDateTime closesAt,
        LatePolicy latePolicy, LocalDateTime lateUntil,
        String createdByName, LocalDateTime createdAt) {
}
