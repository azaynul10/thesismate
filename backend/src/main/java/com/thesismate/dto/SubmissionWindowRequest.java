package com.thesismate.dto;

import java.time.LocalDateTime;

import com.thesismate.model.LatePolicy;
import com.thesismate.model.Phase;
import com.thesismate.model.Track;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** F15: create or reschedule a submission window. lateUntil is needed only for LATE_ALLOWED. */
public record SubmissionWindowRequest(
        @NotBlank @Size(max = 30) String semester,
        @NotNull Phase phase,
        @NotNull Track track,
        @NotNull LocalDateTime opensAt,
        @NotNull LocalDateTime closesAt,
        @NotNull LatePolicy latePolicy,
        LocalDateTime lateUntil) {
}
