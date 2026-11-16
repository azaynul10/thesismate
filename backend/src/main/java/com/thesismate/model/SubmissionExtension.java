package com.thesismate.model;

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

/** F15: a later closing time granted to one group for one window; a reason is required. */
@Entity
@Table(name = "submission_extension")
public class SubmissionExtension {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private StudentGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "window_id", nullable = false)
    private SubmissionWindow window;

    @Column(name = "new_closes_at", nullable = false)
    private LocalDateTime newClosesAt;

    @Column(nullable = false, length = 500)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "granted_by", nullable = false)
    private AppUser grantedBy;

    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt;

    protected SubmissionExtension() {
    }

    public SubmissionExtension(StudentGroup group, SubmissionWindow window, LocalDateTime newClosesAt,
                               String reason, AppUser grantedBy) {
        this.group = group;
        this.window = window;
        this.newClosesAt = newClosesAt;
        this.reason = reason;
        this.grantedBy = grantedBy;
    }

    @PrePersist
    void onCreate() {
        if (grantedAt == null) {
            grantedAt = LocalDateTime.now();
        }
    }

    /** Coordinator changes an existing extension (still one per group and window). */
    public void update(LocalDateTime newClosesAt, String reason, AppUser grantedBy) {
        this.newClosesAt = newClosesAt;
        this.reason = reason;
        this.grantedBy = grantedBy;
    }

    public Long getId() { return id; }
    public StudentGroup getGroup() { return group; }
    public SubmissionWindow getWindow() { return window; }
    public LocalDateTime getNewClosesAt() { return newClosesAt; }
    public String getReason() { return reason; }
    public AppUser getGrantedBy() { return grantedBy; }
    public LocalDateTime getGrantedAt() { return grantedAt; }
}
