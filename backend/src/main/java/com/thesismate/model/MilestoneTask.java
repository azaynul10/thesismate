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

/** F16: one task of a milestone, optionally assigned to a group member. */
@Entity
@Table(name = "milestone_task")
public class MilestoneTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "milestone_id", nullable = false)
    private Milestone milestone;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private AppUser assignee;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status = TaskStatus.TODO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TaskSource source = TaskSource.MANUAL;

    @Column(name = "source_ref", length = 100)
    private String sourceRef;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private AppUser createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected MilestoneTask() {
    }

    public MilestoneTask(Milestone milestone, String title, String description, AppUser assignee,
                         LocalDate dueDate, TaskSource source, String sourceRef, AppUser createdBy) {
        this.milestone = milestone;
        this.title = title;
        this.description = description;
        this.assignee = assignee;
        this.dueDate = dueDate;
        this.source = source;
        this.sourceRef = sourceRef;
        this.createdBy = createdBy;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    public void update(String title, String description, AppUser assignee, LocalDate dueDate, LocalDateTime now) {
        this.title = title;
        this.description = description;
        this.assignee = assignee;
        this.dueDate = dueDate;
        this.updatedAt = now;
    }

    /** DONE records the time it was completed; any other status clears it. */
    public void changeStatus(TaskStatus newStatus, LocalDateTime now) {
        this.status = newStatus;
        this.completedAt = newStatus == TaskStatus.DONE ? now : null;
        this.updatedAt = now;
    }

    public boolean isDone() {
        return status == TaskStatus.DONE;
    }

    public Long getId() { return id; }
    public Milestone getMilestone() { return milestone; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public AppUser getAssignee() { return assignee; }
    public LocalDate getDueDate() { return dueDate; }
    public TaskStatus getStatus() { return status; }
    public TaskSource getSource() { return source; }
    public String getSourceRef() { return sourceRef; }
    public AppUser getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}
