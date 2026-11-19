package com.thesismate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * F17: runs the missing-report reminders every morning. The cron can be changed with
 * thesismate.reminders.weekly-report-cron. The coordinator can also run it by hand (see WeeklyReportController).
 */
@Component
public class WeeklyReportReminderJob {

    private static final Logger log = LoggerFactory.getLogger(WeeklyReportReminderJob.class);

    private final WeeklyReportService service;

    public WeeklyReportReminderJob(WeeklyReportService service) {
        this.service = service;
    }

    @Scheduled(cron = "${thesismate.reminders.weekly-report-cron:0 0 8 * * *}")
    public void run() {
        try {
            int sent = service.sendReminders();
            if (sent > 0) {
                log.info("Weekly report reminders recorded for {} group(s)", sent);
            }
        } catch (RuntimeException e) {
            log.warn("Weekly report reminder job failed", e);
        }
    }
}
