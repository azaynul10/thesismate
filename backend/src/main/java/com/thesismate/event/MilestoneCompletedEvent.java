package com.thesismate.event;

/** F16: the last open task of a milestone was marked DONE, so the milestone reached 100 percent. */
public record MilestoneCompletedEvent(Long milestoneId, Long groupId, String milestoneTitle) {
}
