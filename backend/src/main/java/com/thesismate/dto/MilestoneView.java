package com.thesismate.dto;

import java.time.LocalDate;
import java.util.List;

import com.thesismate.model.Phase;

public record MilestoneView(
        Long id,
        String title,
        String description,
        Phase phase,
        LocalDate dueDate,
        int sortOrder,
        boolean fromTemplate,
        int completionPercent,
        int totalTasks,
        int doneTasks,
        int overdueTasks,
        boolean overdue,
        /** NOT_STARTED, IN_PROGRESS or COMPLETED. */
        String state,
        List<TaskView> tasks) {
}
