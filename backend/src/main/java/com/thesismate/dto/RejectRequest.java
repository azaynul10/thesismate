package com.thesismate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectRequest(
        @NotBlank(message = "A reason is required")
        @Size(max = 500, message = "Reason must be at most 500 characters") String reason) {
}