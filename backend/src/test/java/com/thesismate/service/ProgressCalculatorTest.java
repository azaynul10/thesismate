package com.thesismate.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.thesismate.dto.ProgressSummary;
import com.thesismate.service.ProgressCalculator.MilestoneStats;

class ProgressCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 19);

    @Test
    void percentIsShareOfDoneTasksRoundedToWholeNumber() {
        assertEquals(0, ProgressCalculator.percent(0, 4));
        assertEquals(33, ProgressCalculator.percent(1, 3));
        assertEquals(67, ProgressCalculator.percent(2, 3));
        assertEquals(100, ProgressCalculator.percent(5, 5));
    }

    @Test
    void noTasksMeansZeroPercent() {
        assertEquals(0, ProgressCalculator.percent(0, 0));
    }

    @Test
    void taskIsOverdueOnlyWhenNotDoneAndPastDue() {
        assertTrue(ProgressCalculator.taskOverdue(false, TODAY.minusDays(1), TODAY));
        assertFalse(ProgressCalculator.taskOverdue(false, TODAY, TODAY));
        assertFalse(ProgressCalculator.taskOverdue(true, TODAY.minusDays(5), TODAY));
        assertFalse(ProgressCalculator.taskOverdue(false, null, TODAY));
    }

    @Test
    void milestoneNeedsTasksToBeCompleted() {
        assertFalse(ProgressCalculator.milestoneCompleted(0, 0));
        assertFalse(ProgressCalculator.milestoneCompleted(1, 2));
        assertTrue(ProgressCalculator.milestoneCompleted(2, 2));
    }

    @Test
    void milestoneIsOverdueWhenPastDueAndNotCompleted() {
        assertTrue(ProgressCalculator.milestoneOverdue(false, TODAY.minusDays(1), TODAY));
        assertFalse(ProgressCalculator.milestoneOverdue(true, TODAY.minusDays(1), TODAY));
        assertFalse(ProgressCalculator.milestoneOverdue(false, TODAY, TODAY));
        assertFalse(ProgressCalculator.milestoneOverdue(false, null, TODAY));
    }

    @Test
    void summaryAddsUpAllMilestones() {
        ProgressSummary s = ProgressCalculator.summarize(List.of(
                new MilestoneStats(1, 2, 1, TODAY.minusDays(4)),
                new MilestoneStats(2, 2, 0, null),
                new MilestoneStats(0, 0, 0, TODAY.plusDays(10))), TODAY);

        assertEquals(60, s.completionPercent());
        assertEquals(4, s.totalTasks());
        assertEquals(3, s.doneTasks());
        assertEquals(1, s.overdueTasks());
        assertEquals(3, s.totalMilestones());
        assertEquals(1, s.completedMilestones());
        assertEquals(1, s.overdueMilestones());
    }

    @Test
    void emptyPlanHasAllZeros() {
        ProgressSummary s = ProgressCalculator.summarize(List.of(), TODAY);
        assertEquals(0, s.completionPercent());
        assertEquals(0, s.totalMilestones());
    }
}
