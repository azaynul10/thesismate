package com.thesismate.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class WeekCalendarTest {

    @Test
    void weekStartIsTheMonday() {
        LocalDate monday = LocalDate.of(2026, 11, 16);
        assertEquals(monday, WeekCalendar.weekStart(LocalDate.of(2026, 11, 16)));
        assertEquals(monday, WeekCalendar.weekStart(LocalDate.of(2026, 11, 19)));
        assertEquals(monday, WeekCalendar.weekStart(LocalDate.of(2026, 11, 22)));
        assertEquals(LocalDate.of(2026, 11, 23), WeekCalendar.weekStart(LocalDate.of(2026, 11, 23)));
    }

    @Test
    void weekEndIsTheSunday() {
        assertEquals(LocalDate.of(2026, 11, 22), WeekCalendar.weekEnd(LocalDate.of(2026, 11, 16)));
    }

    @Test
    void onlyMondaysAreWeekStarts() {
        assertTrue(WeekCalendar.isWeekStart(LocalDate.of(2026, 11, 16)));
        assertFalse(WeekCalendar.isWeekStart(LocalDate.of(2026, 11, 17)));
    }

    @Test
    void weeksBetweenIncludesBothEnds() {
        assertEquals(List.of(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 16)),
                WeekCalendar.weeksBetween(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 16)));
        assertEquals(List.of(LocalDate.of(2026, 11, 9)),
                WeekCalendar.weeksBetween(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 9)));
    }

    @Test
    void weeksBetweenIsEmptyWhenLastIsBeforeFirst() {
        assertTrue(WeekCalendar.weeksBetween(LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 9)).isEmpty());
    }
}
