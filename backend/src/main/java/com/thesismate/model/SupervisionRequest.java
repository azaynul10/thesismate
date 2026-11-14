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

/** F7: one pitch from a group to one supervisor. */
@Entity
@Table(name = "supervision_request")
public class SupervisionRequest {

    public enum Status { PENDING, ACCEPTED, REJECTED, WITHDRAWN, EXPIRED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private StudentGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supervisor_id", nullable = false)
    private AppUser supervisor;

    @Enumerated(EnumType.STRING)
    @Column(name = "supervisor_role", nullable = false, length = 20)
    private SupervisorRole supervisorRole;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String pitch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by", nullable = false)
    private AppUser requestedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    protected SupervisionRequest() {
    }

    public SupervisionRequest(StudentGroup group, AppUser supervisor, SupervisorRole supervisorRole,
                              String pitch, AppUser requestedBy, LocalDateTime expiresAt) {
        this.group = group;
        this.supervisor = supervisor;
        this.supervisorRole = supervisorRole;
        this.pitch = pitch;
        this.requestedBy = requestedBy;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void accept() {
        close(Status.ACCEPTED);
    }

    public void reject(String reason) {
        close(Status.REJECTED);
        this.rejectionReason = reason == null ? null : reason.trim();
    }

    public void withdraw() {
        close(Status.WITHDRAWN);
    }

    public void expire() {
        close(Status.EXPIRED);
    }

    private void close(Status newStatus) {
        if (status != Status.PENDING) {
            throw new IllegalStateException("Request is already " + status);
        }
        this.status = newStatus;
        this.respondedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public StudentGroup getGroup() { return group; }
    public AppUser getSupervisor() { return supervisor; }
    public SupervisorRole getSupervisorRole() { return supervisorRole; }
    public String getPitch() { return pitch; }
    public Status getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    public AppUser getRequestedBy() { return requestedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getRespondedAt() { return respondedAt; }
}