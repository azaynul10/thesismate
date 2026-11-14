package com.thesismate.service;

import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.thesismate.model.SupervisorCapacity;
import com.thesismate.model.Track;
import com.thesismate.repository.SupervisorCapacityRepository;
import com.thesismate.repository.SupervisorProfileRepository;

/** Reads a supervisor's tracks and per-semester capacity from their profile. Coordinator overrides are already in the capacity row. */
@Component
public class ProfileSupervisorCapacityLookup implements SupervisorCapacityLookup {

    private final SupervisorProfileRepository profiles;
    private final SupervisorCapacityRepository capacities;

    public ProfileSupervisorCapacityLookup(SupervisorProfileRepository profiles,
                                           SupervisorCapacityRepository capacities) {
        this.profiles = profiles;
        this.capacities = capacities;
    }

    @Override
    @Transactional(readOnly = true)
    public int capacityFor(Long supervisorUserId, String semester) {
        return profiles.findByUserId(supervisorUserId)
                .flatMap(p -> capacities.findByProfileIdAndSemester(p.getId(), semester))
                .map(SupervisorCapacity::getCapacity)
                .orElse(0);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Track> tracksFor(Long supervisorUserId) {
        return profiles.findByUserId(supervisorUserId)
                .map(p -> Set.copyOf(p.getTracks()))
                .orElse(Set.of());
    }
}