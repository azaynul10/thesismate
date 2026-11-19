package com.thesismate.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.thesismate.model.TaskSource;
import com.thesismate.model.TaskStatus;

public record TaskView(
        Long id,
        Long milestoneId,
        String title,
        String description,
        Long assigneeId,
        String assigneeName,
        /** False when the assignee has left the group, so the group can reassign the task. */
        boolean assigneeActive,
        LocalDate dueDate,
        TaskStatus status,
        boolean overdue,
        TaskSource source,
        String sourceRef,
        LocalDateTime createdAt,
        LocalDateTime completedAt) {
}
