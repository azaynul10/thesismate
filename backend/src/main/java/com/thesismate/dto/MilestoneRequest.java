package com.thesismate.dto;

import java.time.LocalDate;

import com.thesismate.model.Phase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** F16: create or edit a milestone. */
public record MilestoneRequest(
        @NotBlank(message = "Title is required") @Size(max = 200, message = "Title can have at most 200 characters") String title,
        @Size(max = 2000, message = "Description can have at most 2000 characters") String description,
        Phase phase,
        LocalDate dueDate) {
}
