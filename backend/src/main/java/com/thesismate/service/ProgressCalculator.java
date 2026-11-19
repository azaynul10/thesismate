package com.thesismate.service;

import java.time.LocalDate;
import java.util.List;

import com.thesismate.dto.ProgressSummary;

/**
 * F16 rules for completion and overdue, kept free of database code so they are easy to test and reuse
 * (F20 analytics reads the same numbers through MilestoneService.progressOf).
 * <ul>
 *   <li>Completion is the share of DONE tasks, rounded to a whole percent. No tasks means 0 percent.</li>
 *   <li>A task is overdue when it is not DONE and its due date is before today.</li>
 *   <li>A milestone is overdue when its due date is before today and it is not completed.
 *       A milestone is completed when it has tasks and all of them are DONE.</li>
 * </ul>
 */
public final class ProgressCalculator {

    private ProgressCalculator() {
    }

    public static int percent(int done, int total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.round(done * 100.0 / total);
    }

    public static boolean taskOverdue(boolean done, LocalDate dueDate, LocalDate today) {
        return !done && dueDate != null && dueDate.isBefore(today);
    }

    public static boolean milestoneCompleted(int done, int total) {
        return total > 0 && done == total;
    }

    public static boolean milestoneOverdue(boolean completed, LocalDate dueDate, LocalDate today) {
        return !completed && dueDate != null && dueDate.isBefore(today);
    }

    /** One milestone's numbers, used to build the group summary. */
    public record MilestoneStats(int doneTasks, int totalTasks, int overdueTasks, LocalDate dueDate) {
    }

    public static ProgressSummary summarize(List<MilestoneStats> milestones, LocalDate today) {
        int done = 0;
        int total = 0;
        int overdueTasks = 0;
        int completed = 0;
        int overdueMilestones = 0;
        for (MilestoneStats m : milestones) {
            done += m.doneTasks();
            total += m.totalTasks();
            overdueTasks += m.overdueTasks();
            boolean isCompleted = milestoneCompleted(m.doneTasks(), m.totalTasks());
            if (isCompleted) {
                completed++;
            }
            if (milestoneOverdue(isCompleted, m.dueDate(), today)) {
                overdueMilestones++;
            }
        }
        return new ProgressSummary(percent(done, total), total, done, overdueTasks,
                milestones.size(), completed, overdueMilestones);
    }
}
