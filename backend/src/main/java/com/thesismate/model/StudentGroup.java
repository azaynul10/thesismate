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

@Entity
@Table(name = "student_group")
public class StudentGroup {

    public enum Status { ACTIVE, DISBANDED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Track track;

    @Column(nullable = false, length = 30)
    private String semester;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leader_id", nullable = false)
    private AppUser leader;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected StudentGroup() {
    }

    public StudentGroup(Track track, String semester, AppUser leader) {
        this.track = track;
        this.semester = semester;
        this.leader = leader;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void transferLeader(AppUser newLeader) {
        this.leader = newLeader;
    }

    public void disband() {
        this.status = Status.DISBANDED;
    }

    public Long getId() { return id; }
    public Track getTrack() { return track; }
    public String getSemester() { return semester; }
    public Status getStatus() { return status; }
    public AppUser getLeader() { return leader; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}