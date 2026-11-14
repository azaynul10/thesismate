package com.thesismate.service;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.thesismate.model.AppUser;
import com.thesismate.model.Role;
import com.thesismate.model.SupervisorCapacity;
import com.thesismate.model.SupervisorProfile;
import com.thesismate.model.Track;
import com.thesismate.repository.SupervisorCapacityRepository;
import com.thesismate.repository.SupervisorProfileRepository;

@ExtendWith(MockitoExtension.class)
class ProfileSupervisorCapacityLookupTest {

    private static final String SEMESTER = "Spring 2027";

    @Mock SupervisorProfileRepository profiles;
    @Mock SupervisorCapacityRepository capacities;

    private ProfileSupervisorCapacityLookup lookup;

    @BeforeEach
    void setUp() {
        lookup = new ProfileSupervisorCapacityLookup(profiles, capacities);
    }

    private SupervisorProfile profile(Set<Track> tracks) {
        AppUser user = new AppUser("sup@thesismate.test", "Dr Sup", Role.SUPERVISOR, "pw", null);
        ReflectionTestUtils.setField(user, "id", 20L);
        SupervisorProfile p = new SupervisorProfile(user, null, null);
        ReflectionTestUtils.setField(p, "id", 7L);
        p.setTracks(tracks);
        return p;
    }

    @Test
    void capacityComesFromTheSemesterRow() {
        SupervisorProfile p = profile(Set.of(Track.THESIS));
        when(profiles.findByUserId(20L)).thenReturn(Optional.of(p));
        when(capacities.findByProfileIdAndSemester(7L, SEMESTER))
                .thenReturn(Optional.of(new SupervisorCapacity(p, SEMESTER, 3)));
        assertEquals(3, lookup.capacityFor(20L, SEMESTER));
    }

    @Test
    void noCapacityRowMeansZero() {
        when(profiles.findByUserId(20L)).thenReturn(Optional.of(profile(Set.of(Track.THESIS))));
        assertEquals(0, lookup.capacityFor(20L, SEMESTER));
    }

    @Test
    void noProfileMeansNoCapacityAndNoTracks() {
        assertEquals(0, lookup.capacityFor(20L, SEMESTER));
        assertTrue(lookup.tracksFor(20L).isEmpty());
    }

    @Test
    void tracksComeFromTheProfile() {
        when(profiles.findByUserId(20L))
                .thenReturn(Optional.of(profile(Set.of(Track.THESIS, Track.PROJECT))));
        assertEquals(Set.of(Track.THESIS, Track.PROJECT), lookup.tracksFor(20L));
    }
}