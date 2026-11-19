package com.thesismate.dto;

import java.time.LocalDate;
import java.util.List;

import com.thesismate.model.SupervisorRole;
import com.thesismate.model.Track;

/** One supervised group on the supervisor progress page: milestone numbers plus weekly report status. */
public record GroupProgressView(
        Long groupId,
        Track track,
        String semester,
        SupervisorRole myRole,
        List<String> memberNames,
        ProgressSummary progress,
        /** False for internship groups. The weekly numbers below are then zero. */
        boolean weeklyReportsApplicable,
        int missingWeeks,
        int pendingReports,
        LocalDate lastReportWeek) {
}
