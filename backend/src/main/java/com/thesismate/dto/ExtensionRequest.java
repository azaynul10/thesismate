package com.thesismate.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** F15: give one group a later closing time for one window. A reason is required. */
public record ExtensionRequest(
        @NotNull Long groupId,
        @NotNull Long windowId,
        @NotNull LocalDateTime newClosesAt,
        @NotBlank @Size(max = 500) String reason) {
}
