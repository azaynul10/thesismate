package com.thesismate.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "capacity_override")
public class CapacityOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private SupervisorProfile profile;

    @Column(nullable = false, length = 30)
    private String semester;

    @Column(name = "old_capacity", nullable = false)
    private int oldCapacity;

    @Column(name = "new_capacity", nullable = false)
    private int newCapacity;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coordinator_id", nullable = false)
    private AppUser coordinator;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected CapacityOverride() {}

    public CapacityOverride(SupervisorProfile profile, String semester,
                            int oldCapacity, int newCapacity, String reason, AppUser coordinator) {
        if (newCapacity < 0)
            throw new IllegalArgumentException("capacity cannot be negative");
        if (reason == null || reason.isBlank())
            throw new IllegalArgumentException("override reason is required");
        this.profile = profile;
        this.semester = semester;
        this.oldCapacity = oldCapacity;
        this.newCapacity = newCapacity;
        this.reason = reason.trim();
        this.coordinator = coordinator;
    }

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public SupervisorProfile getProfile() { return profile; }
    public String getSemester() { return semester; }
    public int getOldCapacity() { return oldCapacity; }
    public int getNewCapacity() { return newCapacity; }
    public String getReason() { return reason; }
    public AppUser getCoordinator() { return coordinator; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}