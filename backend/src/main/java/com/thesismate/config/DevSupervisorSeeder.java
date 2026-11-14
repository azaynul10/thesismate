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
        seed("supervisor1@thesismate.test", "07A-01C",
                "Professor, CSE · Researching Deep Learning, Ambient Intelligence, IoT & Computer Vision.",
                Set.of("Machine Learning", "Computer Vision", "Deep Learning"),
                EnumSet.allOf(Track.class), 2);
        seed("supervisor2@thesismate.test", "07B-02L",
                "Professor, CSE · Focused on AI, Bioinformatics, Metaheuristics & Software Engineering.",
                Set.of("Software Engineering", "Databases", "Bioinformatics"),
                EnumSet.of(Track.THESIS, Track.PROJECT), 1);
        seed("supervisor3@thesismate.test", "08A-03C",
                "Professor, CSE · Working on Cybersecurity, Blockchain, Decentralized Identity & Networks.",
                Set.of("Networks", "Security", "Blockchain"),
                EnumSet.of(Track.PROJECT, Track.INTERNSHIP), 3);
    }

    private void seed(String email, String room, String bio, Set<String> areas, Set<Track> tracks, int capacity) {
        users.findByEmail(email).ifPresent(user -> {
            SupervisorProfile profile = profiles.findByUserId(user.getId()).orElseGet(() -> {
                SupervisorProfile created = new SupervisorProfile(user, bio, room);
                created.setResearchAreas(areas);
                created.setTracks(tracks);
                return profiles.save(created);
            });
            if (profile.getBio() == null || profile.getBio().isBlank()
                    || "Dev supervisor profile".equals(profile.getBio())) {
                profile.setBio(bio);
                profile.setOfficeRoom(room);
                profiles.save(profile);
            }
            if (capacities.findByProfileIdAndSemester(profile.getId(), SEMESTER).isEmpty()) {
                capacities.save(new SupervisorCapacity(profile, SEMESTER, capacity));
            }
        });
    }
}