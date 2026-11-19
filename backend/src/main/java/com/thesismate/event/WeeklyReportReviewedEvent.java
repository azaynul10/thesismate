package com.thesismate.event;

import java.time.LocalDate;
import java.util.List;

import com.thesismate.model.WeeklyReportStatus;

/** F17: a supervisor approved or returned a weekly report. Recipients are the group members. */
public record WeeklyReportReviewedEvent(Long reportId, Long groupId, LocalDate weekStart,
                                        WeeklyReportStatus outcome, String comment, List<Long> memberIds) {
}
