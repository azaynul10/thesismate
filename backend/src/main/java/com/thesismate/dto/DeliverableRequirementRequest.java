package com.thesismate.dto;

import com.thesismate.model.DeliverableType;
import com.thesismate.model.Phase;
import com.thesismate.model.Track;

import jakarta.validation.constraints.NotNull;

/** F15: one checklist line. mandatory defaults to true when left out. */
public record DeliverableRequirementRequest(
        @NotNull Phase phase,
        @NotNull Track track,
        @NotNull DeliverableType deliverableType,
        Boolean mandatory) {
}
