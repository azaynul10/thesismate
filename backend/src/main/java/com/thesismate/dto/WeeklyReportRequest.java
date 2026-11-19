package com.thesismate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** F17: the three parts of a weekly report. Blockers may be empty. */
public record WeeklyReportRequest(
        @NotBlank(message = "Describe what was done this week") @Size(max = 4000, message = "At most 4000 characters") String done,
        @NotBlank(message = "Describe what comes next") @Size(max = 4000, message = "At most 4000 characters") String next,
        @Size(max = 4000, message = "At most 4000 characters") String blockers) {
}
