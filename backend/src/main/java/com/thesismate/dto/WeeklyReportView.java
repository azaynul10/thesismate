package com.thesismate.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.thesismate.model.Track;
import com.thesismate.model.WeeklyReportStatus;

public record WeeklyReportView(
        Long id,
        Long groupId,
        Track track,
        String semester,
        /** Names of the group members, so a supervisor sees whose report it is. */
        java.util.List<String> memberNames,
        LocalDate weekStart,
        LocalDate weekEnd,
        String done,
        String next,
        String blockers,
        WeeklyReportStatus status,
        /** True when it was handed in after the Sunday that closes the week. */
        boolean late,
        String submittedByName,
        LocalDateTime submittedAt,
        String reviewedByName,
        LocalDateTime reviewedAt,
        String reviewComment) {
}
