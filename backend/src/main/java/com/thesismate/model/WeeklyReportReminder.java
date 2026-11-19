package com.thesismate.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** F17: records that a missing-report reminder was already sent for one group and week. */
@Entity
@Table(name = "weekly_report_reminder")
public class WeeklyReportReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private StudentGroup group;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected WeeklyReportReminder() {
    }

    public WeeklyReportReminder(StudentGroup group, LocalDate weekStart) {
        this.group = group;
        this.weekStart = weekStart;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public StudentGroup getGroup() { return group; }
    public LocalDate getWeekStart() { return weekStart; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
