package com.thesismate.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OverrideCapacityRequest(
        @NotBlank @Size(max = 30) String semester,
        @Min(0) int newCapacity,
        @NotBlank @Size(max = 500) String reason) {
}
