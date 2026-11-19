package com.thesismate.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.time.temporal.TemporalAdjusters;

/**
 * The week rules shared by features that work week by week (F17 weekly reports, F22 internship logbook later).
 * A week runs from Monday to Sunday and is identified by its Monday.
 */
public final class WeekCalendar {

    private WeekCalendar() {
    }

    /** The Monday of the week that contains {@code date}. */
    public static LocalDate weekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public static boolean isWeekStart(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.MONDAY;
    }

    /** The Sunday that closes the week starting on {@code weekStart}. */
    public static LocalDate weekEnd(LocalDate weekStart) {
        return weekStart.plusDays(6);
    }

    /** Every Monday from {@code first} to {@code last}, both included. Empty when last is before first. */
    public static List<LocalDate> weeksBetween(LocalDate first, LocalDate last) {
        List<LocalDate> weeks = new ArrayList<>();
        for (LocalDate w = first; !w.isAfter(last); w = w.plusWeeks(1)) {
            weeks.add(w);
        }
        return weeks;
    }
}
