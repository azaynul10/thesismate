-- V10 (F17): a group may hand in several reports for the same week, and approved weeks stay open.
-- Each report is now its own row, so the one-report-per-week rule from V9 goes away.
ALTER TABLE weekly_report DROP CONSTRAINT uq_weekly_report_group_week;
CREATE INDEX idx_weekly_report_group_week ON weekly_report (group_id, week_start, submitted_at);
