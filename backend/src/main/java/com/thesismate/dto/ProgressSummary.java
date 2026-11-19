package com.thesismate.dto;

/**
 * F16: calculated progress of one group (or one supervisor's view of it).
 * F20 analytics reuses this record through MilestoneService.progressOf.
 */
public record ProgressSummary(
        int completionPercent,
        int totalTasks,
        int doneTasks,
        int overdueTasks,
        int totalMilestones,
        int completedMilestones,
        int overdueMilestones) {
}
