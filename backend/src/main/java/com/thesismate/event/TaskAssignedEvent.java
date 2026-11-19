package com.thesismate.event;

/** F16: a task was assigned to a group member (on create or when the assignee changed). */
public record TaskAssignedEvent(Long taskId, Long groupId, String taskTitle, Long assigneeId, Long assignedById) {
}
