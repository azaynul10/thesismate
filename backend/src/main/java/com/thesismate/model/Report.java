package com.thesismate.model;

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

/** F11: the report of one group for one phase. Its uploads are kept as ReportVersion rows. */
@Entity
@Table(name = "report")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private StudentGroup group;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Phase phase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReportStatus status = ReportStatus.SUBMITTED;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Report() {
    }

    public Report(StudentGroup group, Phase phase) {
        this.group = group;
        this.phase = phase;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /** An approved report can no longer be replaced by a new upload. */
    public boolean isLocked() {
        return status == ReportStatus.SUPERVISOR_APPROVED || status == ReportStatus.COMMITTEE_APPROVED;
    }

    /** A new upload puts the report back to SUBMITTED (for example after a revision was required). */
    public void markSubmitted() {
        this.status = ReportStatus.SUBMITTED;
    }

    /** Used by the approval workflow (F13, Sprint 3). */
    public void changeStatus(ReportStatus status) {
        this.status = status;
    }

    public Long getId() { return id; }
    public StudentGroup getGroup() { return group; }
    public Phase getPhase() { return phase; }
    public ReportStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
