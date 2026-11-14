package com.thesismate.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.*;

@Entity
@Table(name = "supervisor_profile")
public class SupervisorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "office_room", length = 100)
    private String officeRoom;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ElementCollection
    @CollectionTable(name = "supervisor_research_area", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "area", nullable = false, length = 120)
    private Set<String> researchAreas = new HashSet<>();

    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "supervisor_track", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "track", nullable = false, length = 20)
    private Set<Track> tracks = new HashSet<>();

    protected SupervisorProfile() {}

    public SupervisorProfile(AppUser user, String bio, String officeRoom) {
        this.user = user;
        this.bio = bio;
        this.officeRoom = officeRoom;
    }

    @PrePersist
    @PreUpdate
    void touch() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public AppUser getUser() { return user; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getOfficeRoom() { return officeRoom; }
    public void setOfficeRoom(String officeRoom) { this.officeRoom = officeRoom; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public Set<String> getResearchAreas() { return researchAreas; }
    public void setResearchAreas(Set<String> researchAreas) {
        this.researchAreas.clear();
        if (researchAreas != null) {
            this.researchAreas.addAll(researchAreas);
        }
    }

    public Set<Track> getTracks() { return tracks; }
    public void setTracks(Set<Track> tracks) {
        this.tracks.clear();
        if (tracks != null) {
            this.tracks.addAll(tracks);
        }
    }
}