package com.thesismate.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.thesismate.dto.CapacityOverrideView;
import com.thesismate.dto.OverrideCapacityRequest;
import com.thesismate.dto.SupervisorCapacityView;
import com.thesismate.dto.UpdateCapacityRequest;
import com.thesismate.dto.UpdateProfileRequest;
import com.thesismate.model.AppUser;
import com.thesismate.model.CapacityOverride;
import com.thesismate.model.SupervisorCapacity;
import com.thesismate.model.SupervisorProfile;
import com.thesismate.repository.AppUserRepository;
import com.thesismate.repository.CapacityOverrideRepository;
import com.thesismate.repository.SupervisorCapacityRepository;
import com.thesismate.repository.SupervisorProfileRepository;

// F1 er business rules: profile, semester capacity ar coordinator override
@Service
public class SupervisorCapacityService {

    private final SupervisorProfileRepository profiles;
    private final SupervisorCapacityRepository capacities;
    private final CapacityOverrideRepository overrides;
    private final AppUserRepository users;
    private final AllocationCountProvider allocationCountProvider;

    public SupervisorCapacityService(SupervisorProfileRepository profiles,
                                     SupervisorCapacityRepository capacities,
                                     CapacityOverrideRepository overrides,
                                     AppUserRepository users,
                                     AllocationCountProvider allocationCountProvider) {
        this.profiles = profiles;
        this.capacities = capacities;
        this.overrides = overrides;
        this.users = users;
        this.allocationCountProvider = allocationCountProvider;
    }

    // login kora supervisor er nijer profile ar capacity dekha ekhane
    @Transactional(readOnly = true)
    public SupervisorCapacityView getMyCapacity(String email, String semester) {
        String sem = normalizeSemester(semester);
        SupervisorProfile profile = profiles.findByUserEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supervisor profile not found"));
        SupervisorCapacity cap = capacities.findByProfileIdAndSemester(profile.getId(), sem).orElse(null);
        return toView(profile, cap, sem);
    }

    // supervisor nijer profile update kore, na thakle notun ta banay
    @Transactional
    public SupervisorCapacityView updateProfile(String email, String semester, UpdateProfileRequest req) {
        String sem = normalizeSemester(semester);
        SupervisorProfile profile = getOrCreateProfile(email);
        profile.setBio(req.bio());
        profile.setOfficeRoom(req.officeRoom());
        profile.setResearchAreas(req.researchAreas().stream().map(String::trim).collect(Collectors.toSet()));
        profile.setTracks(req.tracks());
        SupervisorCapacity cap = capacities.findByProfileIdAndSemester(profile.getId(), sem).orElse(null);
        return toView(profile, cap, sem);
    }

    // semester er capacity set kore; notun row insert, purano row e version match lagbe
    @Transactional
    public SupervisorCapacityView updateCapacity(String email, UpdateCapacityRequest req) {
        String sem = normalizeSemester(req.semester());
        SupervisorProfile profile = getOrCreateProfile(email);
        SupervisorCapacity cap = capacities
                .findByProfileIdAndSemester(profile.getId(), sem)
                .orElse(null);
        if (cap == null) {
            cap = new SupervisorCapacity(profile, sem, req.capacity());
        } else {
            if (req.version() == null || !req.version().equals(cap.getVersion())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Capacity was updated by another user. Refresh and try again.");
            }
            cap.setCapacity(req.capacity());
        }
        // saveAndFlush kori jate db er notun version response e jay
        cap = capacities.saveAndFlush(cap);
        return toView(profile, cap, sem);
    }

    // coordinator capacity badlay, override er log row o ek transaction e dhuke
    @Transactional
    public SupervisorCapacityView overrideCapacity(Long profileId, String coordinatorEmail, OverrideCapacityRequest req) {
        String sem = normalizeSemester(req.semester());
        SupervisorProfile profile = profiles.findById(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supervisor profile not found"));
        AppUser coordinator = users.findByEmail(coordinatorEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Coordinator not found"));

        SupervisorCapacity cap = capacities
                .findByProfileIdAndSemester(profileId, sem)
                .orElse(new SupervisorCapacity(profile, sem, 0));
        int old = cap.getCapacity();
        cap.setCapacity(req.newCapacity());
        cap = capacities.saveAndFlush(cap);
        overrides.save(new CapacityOverride(
                profile, sem, old, req.newCapacity(), req.reason().trim(), coordinator));
        return toView(profile, cap, sem);
    }

    private String normalizeSemester(String semester) {
        if (semester == null || semester.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Semester is required");
        }
        return semester.trim();
    }

    // ei supervisor er override history, notun ta age
    @Transactional(readOnly = true)
    public List<CapacityOverrideView> getOverrides(Long profileId) {
        return overrides.findByProfileIdOrderByCreatedAtDesc(profileId).stream()
                .map(o -> new CapacityOverrideView(o.getId(), o.getSemester(),
                        o.getOldCapacity(), o.getNewCapacity(), o.getReason(),
                        o.getCoordinator().getFullName(), o.getCreatedAt()))
                .toList();
    }

    private SupervisorProfile getOrCreateProfile(String email) {
        return profiles.findByUserEmail(email).orElseGet(() -> {
            AppUser user = users.findByEmail(email)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
            return profiles.save(new SupervisorProfile(user, null, null));
        });
    }

    // used ar remaining sob somoy calculate hoy, db te store kora na
    private SupervisorCapacityView toView(SupervisorProfile profile, SupervisorCapacity cap, String semester) {
        long used = allocationCountProvider.countActiveAllocations(profile.getUser().getId(), semester);
        int capacity = cap == null ? 0 : cap.getCapacity();
        long remaining = Math.max(0L, capacity - used);
        // lazy collection gulo copy kori, naile transaction sesher pore serialize e error ashe
        return new SupervisorCapacityView(
                profile.getId(), profile.getUser().getFullName(), profile.getBio(), profile.getOfficeRoom(),
                Set.copyOf(profile.getResearchAreas()), Set.copyOf(profile.getTracks()),
                semester, capacity, used, remaining, remaining > 0,
                cap == null ? null : cap.getVersion());
    }
}
