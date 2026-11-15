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

/** F15: when groups of one track may submit the report of one phase in one semester. */
@Entity
@Table(name = "submission_window")
public class SubmissionWindow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String semester;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Phase phase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Track track;

    @Column(name = "opens_at", nullable = false)
    private LocalDateTime opensAt;

    @Column(name = "closes_at", nullable = false)
    private LocalDateTime closesAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "late_policy", nullable = false, length = 20)
    private LatePolicy latePolicy = LatePolicy.NO_LATE;

    @Column(name = "late_until")
    private LocalDateTime lateUntil;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private AppUser createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected SubmissionWindow() {
    }

    public SubmissionWindow(String semester, Phase phase, Track track, LocalDateTime opensAt,
                            LocalDateTime closesAt, LatePolicy latePolicy, LocalDateTime lateUntil,
                            AppUser createdBy) {
        this.semester = semester;
        this.phase = phase;
        this.track = track;
        this.opensAt = opensAt;
        this.closesAt = closesAt;
        this.latePolicy = latePolicy;
        this.lateUntil = lateUntil;
        this.createdBy = createdBy;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /** Coordinator edits the times and late policy; semester, phase and track stay fixed. */
    public void reschedule(LocalDateTime opensAt, LocalDateTime closesAt, LatePolicy latePolicy,
                           LocalDateTime lateUntil) {
        this.opensAt = opensAt;
        this.closesAt = closesAt;
        this.latePolicy = latePolicy;
        this.lateUntil = lateUntil;
    }

    public Long getId() { return id; }
    public String getSemester() { return semester; }
    public Phase getPhase() { return phase; }
    public Track getTrack() { return track; }
    public LocalDateTime getOpensAt() { return opensAt; }
    public LocalDateTime getClosesAt() { return closesAt; }
    public LatePolicy getLatePolicy() { return latePolicy; }
    public LocalDateTime getLateUntil() { return lateUntil; }
    public AppUser getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
