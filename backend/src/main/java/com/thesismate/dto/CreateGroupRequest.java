package com.thesismate.dto;

import com.thesismate.model.Track;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateGroupRequest(
        @NotNull(message = "Track is required") Track track,
        @NotBlank(message = "Semester is required")
        @Size(max = 30, message = "Semester must be at most 30 characters") String semester) {
}