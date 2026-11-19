package com.thesismate.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** F16: create or edit a task. assigneeId must be an active member of the group, or empty. */
public record TaskRequest(
        @NotBlank(message = "Title is required") @Size(max = 200, message = "Title can have at most 200 characters") String title,
        @Size(max = 2000, message = "Description can have at most 2000 characters") String description,
        Long assigneeId,
        LocalDate dueDate) {
}
