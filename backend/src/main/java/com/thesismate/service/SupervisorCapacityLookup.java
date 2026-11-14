package com.thesismate.service;

import java.util.Set;

import com.thesismate.model.Track;

/** What F7 needs to know about a supervisor. Backed by F1's tables once F1 is merged. */
public interface SupervisorCapacityLookup {

    /** Capacity for the semester (including coordinator overrides). 0 if the supervisor set none. */
    int capacityFor(Long supervisorUserId, String semester);

    /** The tracks this supervisor takes. */
    Set<Track> tracksFor(Long supervisorUserId);
}