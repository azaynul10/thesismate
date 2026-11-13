package com.thesismate.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.thesismate.dto.OverrideCapacityRequest;
import com.thesismate.dto.UpdateCapacityRequest;
import com.thesismate.model.AppUser;
import com.thesismate.model.CapacityOverride;
import com.thesismate.model.Role;
import com.thesismate.model.SupervisorCapacity;
import com.thesismate.model.SupervisorProfile;
import com.thesismate.repository.AppUserRepository;
import com.thesismate.repository.CapacityOverrideRepository;
import com.thesismate.repository.SupervisorCapacityRepository;
import com.thesismate.repository.SupervisorProfileRepository;

@ExtendWith(MockitoExtension.class)
class SupervisorCapacityServiceTest {

    @Mock SupervisorProfileRepository profiles;
    @Mock SupervisorCapacityRepository capacities;
    @Mock CapacityOverrideRepository overrides;
    @Mock AppUserRepository users;
    @Mock AllocationCountProvider counts;
    @InjectMocks SupervisorCapacityService service;

    private SupervisorProfile profile() {
        AppUser user = new AppUser("sup@t.test", "Sup One", Role.SUPERVISOR, "x", null);
        ReflectionTestUtils.setField(user, "id", 7L);
        SupervisorProfile profile = new SupervisorProfile(user, "bio", "07A-01C");
        ReflectionTestUtils.setField(profile, "id", 1L);
        return profile;
    }

    @Test
    void remainingAndAvailableComeFromAllocationCount() {
        SupervisorProfile profile = profile();
        SupervisorCapacity cap = new SupervisorCapacity(profile, "Fall 2026", 3);
        when(profiles.findByUserEmail("sup@t.test")).thenReturn(Optional.of(profile));
        when(capacities.findByProfileIdAndSemester(1L, "Fall 2026")).thenReturn(Optional.of(cap));
        when(counts.countActiveAllocations(7L, "Fall 2026")).thenReturn(2L);

        var view = service.getMyCapacity("sup@t.test", "Fall 2026");
        assertEquals(2, view.usedSlots());
        assertEquals(1, view.remainingSlots());
        assertTrue(view.available());
    }

    @Test
    void fullSupervisorIsNotAvailable() {
        SupervisorProfile profile = profile();
        SupervisorCapacity cap = new SupervisorCapacity(profile, "Fall 2026", 2);
        when(profiles.findByUserEmail("sup@t.test")).thenReturn(Optional.of(profile));
        when(capacities.findByProfileIdAndSemester(1L, "Fall 2026")).thenReturn(Optional.of(cap));
        when(counts.countActiveAllocations(7L, "Fall 2026")).thenReturn(2L);

        var view = service.getMyCapacity("sup@t.test", "Fall 2026");
        assertEquals(0, view.remainingSlots());
        assertEquals(false, view.available());
    }

    @Test
    void staleVersionIsRejectedWith409() {
        SupervisorProfile profile = profile();
        SupervisorCapacity cap = new SupervisorCapacity(profile, "Fall 2026", 3);
        ReflectionTestUtils.setField(cap, "version", 5L);
        when(profiles.findByUserEmail("sup@t.test")).thenReturn(Optional.of(profile));
        when(capacities.findByProfileIdAndSemester(1L, "Fall 2026")).thenReturn(Optional.of(cap));

        var ex = assertThrows(ResponseStatusException.class,
                () -> service.updateCapacity("sup@t.test", new UpdateCapacityRequest("Fall 2026", 9, 1L)));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(capacities, never()).saveAndFlush(any());
    }

    @Test
    void missingVersionOnExistingRowIsRejected() {
        SupervisorProfile profile = profile();
        SupervisorCapacity cap = new SupervisorCapacity(profile, "Fall 2026", 3);
        ReflectionTestUtils.setField(cap, "version", 0L);
        when(profiles.findByUserEmail("sup@t.test")).thenReturn(Optional.of(profile));
        when(capacities.findByProfileIdAndSemester(1L, "Fall 2026")).thenReturn(Optional.of(cap));

        assertThrows(ResponseStatusException.class,
                () -> service.updateCapacity("sup@t.test", new UpdateCapacityRequest("Fall 2026", 9, null)));
    }

    @Test
    void overrideUpdatesCapacityAndWritesAuditRow() {
        SupervisorProfile profile = profile();
        SupervisorCapacity cap = new SupervisorCapacity(profile, "Fall 2026", 3);
        AppUser coord = new AppUser("coord@t.test", "Coord", Role.COORDINATOR, "x", null);
        when(profiles.findById(1L)).thenReturn(Optional.of(profile));
        when(users.findByEmail("coord@t.test")).thenReturn(Optional.of(coord));
        when(capacities.findByProfileIdAndSemester(1L, "Fall 2026")).thenReturn(Optional.of(cap));
        when(capacities.saveAndFlush(any(SupervisorCapacity.class))).thenAnswer(i -> i.getArgument(0));
        when(counts.countActiveAllocations(7L, "Fall 2026")).thenReturn(0L);

        var view = service.overrideCapacity(1L, "coord@t.test",
                new OverrideCapacityRequest("Fall 2026", 6, "  extra slot  "));

        assertEquals(6, view.capacity());
        verify(overrides).save(argThat((CapacityOverride o) ->
                o.getOldCapacity() == 3 && o.getNewCapacity() == 6
                        && o.getReason().equals("extra slot") && o.getCoordinator() == coord));
    }

    @Test
    void blankOverrideReasonIsRejectedByEntity() {
        SupervisorProfile profile = profile();
        assertThrows(IllegalArgumentException.class,
                () -> new CapacityOverride(profile, "Fall 2026", 3, 6, "   ", null));
    }
}
