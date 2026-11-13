package com.thesismate.dto;

import java.util.Set;

import com.thesismate.model.Track;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 2000) String bio,
        @Size(max = 100) String officeRoom,
        @NotEmpty Set<@NotBlank @Size(max = 120) String> researchAreas,
        @NotEmpty Set<@NotNull Track> tracks) {
}
