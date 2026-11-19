package com.thesismate.dto;

import jakarta.validation.constraints.Size;

/** F17: comment for approving (optional) or reason for returning (required, checked in the service). */
public record WeeklyReviewRequest(@Size(max = 2000, message = "At most 2000 characters") String comment) {
}
