package com.thesismate.dto;

import com.thesismate.model.TaskStatus;

import jakarta.validation.constraints.NotNull;

public record TaskStatusRequest(@NotNull(message = "Status is required") TaskStatus status) {
}
