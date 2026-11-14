package com.thesismate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record InviteRequest(
        @NotBlank(message = "Student ID is required")
        @Pattern(regexp = "\\d{8}", message = "Student ID must be 8 digits") String studentId) {
}