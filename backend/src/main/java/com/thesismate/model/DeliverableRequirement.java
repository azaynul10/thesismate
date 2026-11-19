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

/** F15: one line of the checklist of what a track must hand in for a phase. */
@Entity
@Table(name = "deliverable_requirement")
public class DeliverableRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Phase phase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Track track;

    @Enumerated(EnumType.STRING)
    @Column(name = "deliverable_type", nullable = false, length = 30)
    private DeliverableType deliverableType;

    @Column(nullable = false)
    private boolean mandatory = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private AppUser createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected DeliverableRequirement() {
    }

    public DeliverableRequirement(Phase phase, Track track, DeliverableType deliverableType,
                                  boolean mandatory, AppUser createdBy) {
        this.phase = phase;
        this.track = track;
        this.deliverableType = deliverableType;
        this.mandatory = mandatory;
        this.createdBy = createdBy;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public Long getId() { return id; }
    public Phase getPhase() { return phase; }
    public Track getTrack() { return track; }
    public DeliverableType getDeliverableType() { return deliverableType; }
    public boolean isMandatory() { return mandatory; }
    public AppUser getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
