package com.thesismate.event;

import java.time.LocalDate;
import java.util.List;

/** F17: a weekly report is waiting for sign-off. Recipients are the group's active supervisors. */
public record WeeklyReportSubmittedEvent(Long reportId, Long groupId, LocalDate weekStart,
                                         List<Long> supervisorIds) {
}
