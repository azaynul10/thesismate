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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** F7: a supervisor (or co-supervisor) assigned to a group. Created only when a request is accepted. */
@Entity
@Table(name = "allocation")
public class Allocation {

    public enum Status { ACTIVE, ENDED }

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

    @Column(nullable = false, length = 30)
    private String semester;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false, unique = true)
    private SupervisionRequest request;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Allocation() {
    }

    public Allocation(StudentGroup group, AppUser supervisor, SupervisorRole supervisorRole,
                      String semester, SupervisionRequest request) {
        this.group = group;
        this.supervisor = supervisor;
        this.supervisorRole = supervisorRole;
        this.semester = semester;
        this.request = request;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void end() {
        this.status = Status.ENDED;
    }

    public Long getId() { return id; }
    public StudentGroup getGroup() { return group; }
    public AppUser getSupervisor() { return supervisor; }
    public SupervisorRole getSupervisorRole() { return supervisorRole; }
    public String getSemester() { return semester; }
    public Status getStatus() { return status; }
    public SupervisionRequest getRequest() { return request; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}