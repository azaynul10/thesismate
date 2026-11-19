package com.thesismate.dto;

import java.time.LocalDate;
import java.util.List;

import com.thesismate.model.Track;

/** F17: everything the weekly report page of a group needs. */
public record WeeklyOverviewView(
        Long groupId,
        Track track,
        /** False for internship groups, which do not use weekly reports. */
        boolean applicable,
        /** True when a report may be handed in now (applicable, active group, has a supervisor). */
        boolean canSubmit,
        /** Plain sentence explaining why reports are not available, or null. */
        String notice,
        /** First week that must have a report (week of the first supervisor), or null before that. */
        LocalDate trackingStartsOn,
        LocalDate currentWeek,
        boolean currentWeekSubmitted,
        /** Past tracked weeks with no report at all, oldest first. */
        List<LocalDate> missingWeeks,
        int returnedCount,
        List<WeeklyReportView> reports) {
}
