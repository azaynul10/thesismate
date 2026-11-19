package com.thesismate.repository;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thesismate.model.WeeklyReportReminder;

public interface WeeklyReportReminderRepository extends JpaRepository<WeeklyReportReminder, Long> {

    boolean existsByGroup_IdAndWeekStart(Long groupId, LocalDate weekStart);
}
