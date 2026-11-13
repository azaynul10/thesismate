package com.thesismate.model;

import jakarta.persistence.*;

@Entity
@Table(
    name = "supervisor_capacity",
    uniqueConstraints = @UniqueConstraint(columnNames = {"profile_id", "semester"})
)
public class SupervisorCapacity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private SupervisorProfile profile;

    @Column(nullable = false, length = 30)
    private String semester;

    @Column(nullable = false)
    private int capacity;

    @Version
    @Column(nullable = false)
    private Long version;

    protected SupervisorCapacity() {}

    public SupervisorCapacity(SupervisorProfile profile, String semester, int capacity) {
        this.profile = profile;
        this.semester = semester;
        setCapacity(capacity);
    }

    public Long getId() { return id; }
    public SupervisorProfile getProfile() { return profile; }
    public String getSemester() { return semester; }
    public int getCapacity() { return capacity; }
    public Long getVersion() { return version; }

    public void setCapacity(int capacity) {
        if (capacity < 0) throw new IllegalArgumentException("capacity cannot be negative");
        this.capacity = capacity;
    }
}