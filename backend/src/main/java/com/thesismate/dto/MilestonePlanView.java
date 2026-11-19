package com.thesismate.dto;

import java.util.List;

import com.thesismate.model.Track;

public record MilestonePlanView(
        Long groupId,
        Track track,
        String semester,
        /** True when the viewer is a member of an active group, so the plan may be changed. */
        boolean canEdit,
        /** True once the track template was copied into the plan. */
        boolean templateApplied,
        ProgressSummary summary,
        List<MilestoneView> milestones) {
}
