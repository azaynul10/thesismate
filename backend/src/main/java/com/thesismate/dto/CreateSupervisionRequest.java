package com.thesismate.dto;

import com.thesismate.model.SupervisorRole;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSupervisionRequest(
        @NotNull(message = "Supervisor is required") Long supervisorId,
        @NotNull(message = "Role is required") SupervisorRole role,
        @NotBlank(message = "Pitch is required")
        @Size(max = 2000, message = "Pitch must be at most 2000 characters") String pitch) {
}