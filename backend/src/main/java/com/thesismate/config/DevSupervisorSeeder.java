package com.thesismate.config;

import java.util.EnumSet;
import java.util.Set;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.thesismate.model.SupervisorCapacity;
import com.thesismate.model.SupervisorProfile;
import com.thesismate.model.Track;
import com.thesismate.repository.AppUserRepository;
import com.thesismate.repository.SupervisorCapacityRepository;
import com.thesismate.repository.SupervisorProfileRepository;

/**
 * Dev-only profiles for the seeded supervisors, so supervision requests can be tried end to end.
 * supervisor1 takes every track with 2 slots, supervisor2 takes thesis and project with 1 slot,
 * supervisor3 takes project and internship with 3 slots. Safe to run repeatedly.
 * Runs once the application has started, so the seeded accounts already exist.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "thesismate.seed", havingValue = "true")
public class DevSupervisorSeeder {

    static final String SEMESTER = "Spring 2027";

    private final AppUserRepository users;
    private final SupervisorProfileRepository profiles;
    private final SupervisorCapacityRepository capacities;

    public DevSupervisorSeeder(AppUserRepository users,
                               SupervisorProfileRepository profiles,
                               SupervisorCapacityRepository capacities) {
        this.users = users;
        this.profiles = profiles;
        this.capacities = capacities;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        seed("supervisor1@thesismate.test", "07A-01C", Set.of("Machine Learning", "Computer Vision"),
                EnumSet.allOf(Track.class), 2);
        seed("supervisor2@thesismate.test", "07B-02A", Set.of("Software Engineering", "Databases"),
                EnumSet.of(Track.THESIS, Track.PROJECT), 1);
        seed("supervisor3@thesismate.test", "08A-03B", Set.of("Networks", "Security"),
                EnumSet.of(Track.PROJECT, Track.INTERNSHIP), 3);
    }

    private void seed(String email, String room, Set<String> areas, Set<Track> tracks, int capacity) {
        users.findByEmail(email).ifPresent(user -> {
            if (profiles.findByUserId(user.getId()).isPresent()) {
                return;
            }
            SupervisorProfile profile = new SupervisorProfile(user, "Dev supervisor profile", room);
            profile.setResearchAreas(areas);
            profile.setTracks(tracks);
            SupervisorProfile saved = profiles.save(profile);
            capacities.save(new SupervisorCapacity(saved, SEMESTER, capacity));
        });
    }
}