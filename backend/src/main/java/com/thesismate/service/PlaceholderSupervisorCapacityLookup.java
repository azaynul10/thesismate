package com.thesismate.service;

import java.util.EnumSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.thesismate.model.Track;

/**
 * TEMPORARY: used until F1 (supervisor capacity) is merged.
 * Treats every supervisor as taking all tracks with unlimited capacity.
 * Replace with an implementation backed by SupervisorProfile and SupervisorCapacity.
 */
@Component
public class PlaceholderSupervisorCapacityLookup implements SupervisorCapacityLookup {

    @Override
    public int capacityFor(Long supervisorUserId, String semester) {
        return Integer.MAX_VALUE;
    }

    @Override
    public Set<Track> tracksFor(Long supervisorUserId) {
        return EnumSet.allOf(Track.class);
    }
}