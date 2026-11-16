package com.thesismate.dto;

import com.thesismate.model.DeliverableType;
import com.thesismate.model.Phase;
import com.thesismate.model.Track;

public record DeliverableRequirementView(
        Long id, Phase phase, Track track, DeliverableType deliverableType, boolean mandatory) {
}
