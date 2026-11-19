package com.thesismate.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** F17: one weekly progress report of a group for a week (identified by the Monday). A group may send several per week. */
@Entity
@Table(name = "weekly_report")
public class WeeklyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private StudentGroup group;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String done;

    @Column(name = "next_plan", nullable = false, columnDefinition = "TEXT")
    private String nextPlan;

    @Column(columnDefinition = "TEXT")
    private String blockers;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WeeklyReportStatus status = WeeklyReportStatus.SUBMITTED;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submitted_by", nullable = false)
    private AppUser submittedBy;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private AppUser reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;

    @Version
    @Column(nullable = false)
    private Long version;

    protected WeeklyReport() {
    }

    public WeeklyReport(StudentGroup group, LocalDate weekStart, String done, String nextPlan, String blockers,
                        AppUser submittedBy, LocalDateTime submittedAt) {
        this.group = group;
        this.weekStart = weekStart;
        this.done = done;
        this.nextPlan = nextPlan;
        this.blockers = blockers;
        this.submittedBy = submittedBy;
        this.submittedAt = submittedAt;
    }

    @PrePersist
    void onCreate() {
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
    }

    public void approve(AppUser supervisor, String comment, LocalDateTime now) {
        review(WeeklyReportStatus.APPROVED, supervisor, comment, now);
    }

    public void returnToGroup(AppUser supervisor, String reason, LocalDateTime now) {
        review(WeeklyReportStatus.RETURNED, supervisor, reason, now);
    }

    private void review(WeeklyReportStatus outcome, AppUser supervisor, String comment, LocalDateTime now) {
        this.status = outcome;
        this.reviewedBy = supervisor;
        this.reviewedAt = now;
        this.reviewComment = comment;
    }

    public Long getId() { return id; }
    public StudentGroup getGroup() { return group; }
    public LocalDate getWeekStart() { return weekStart; }
    public String getDone() { return done; }
    public String getNextPlan() { return nextPlan; }
    public String getBlockers() { return blockers; }
    public WeeklyReportStatus getStatus() { return status; }
    public AppUser getSubmittedBy() { return submittedBy; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public AppUser getReviewedBy() { return reviewedBy; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public String getReviewComment() { return reviewComment; }
}
