package com.thesismate.event;

import java.time.LocalDate;
import java.util.List;

/** F17: a group has not handed in the report of {@code weekStart}. Recipients are the group members. */
public record WeeklyReportReminderEvent(Long groupId, LocalDate weekStart, int missingWeeks, List<Long> memberIds) {
}
