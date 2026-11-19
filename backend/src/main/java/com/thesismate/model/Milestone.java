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

/** F16: one milestone in a group's plan. Its completion is calculated from its tasks. */
@Entity
@Table(name = "milestone")
public class Milestone {

    /** template_key of the bucket that collects tasks created by other features. */
    public static final String FOLLOW_UPS_KEY = "FOLLOW_UPS";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private StudentGroup group;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** The CSE400 phase this milestone belongs to. Empty for milestones that fit no phase. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Phase phase;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "from_template", nullable = false)
    private boolean fromTemplate;

    @Column(name = "template_key", length = 60)
    private String templateKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private AppUser createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Milestone() {
    }

    public Milestone(StudentGroup group, String title, String description, Phase phase, LocalDate dueDate,
                     int sortOrder, boolean fromTemplate, String templateKey, AppUser createdBy) {
        this.group = group;
        this.title = title;
        this.description = description;
        this.phase = phase;
        this.dueDate = dueDate;
        this.sortOrder = sortOrder;
        this.fromTemplate = fromTemplate;
        this.templateKey = templateKey;
        this.createdBy = createdBy;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void update(String title, String description, Phase phase, LocalDate dueDate) {
        this.title = title;
        this.description = description;
        this.phase = phase;
        this.dueDate = dueDate;
    }

    public Long getId() { return id; }
    public StudentGroup getGroup() { return group; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Phase getPhase() { return phase; }
    public LocalDate getDueDate() { return dueDate; }
    public int getSortOrder() { return sortOrder; }
    public boolean isFromTemplate() { return fromTemplate; }
    public String getTemplateKey() { return templateKey; }
    public AppUser getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
