package com.thesismate.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.thesismate.dto.CapacityOverrideView;
import com.thesismate.dto.OverrideCapacityRequest;
import com.thesismate.dto.SupervisorCapacityView;
import com.thesismate.dto.UpdateCapacityRequest;
import com.thesismate.dto.UpdateProfileRequest;
import com.thesismate.service.SupervisorCapacityService;

import jakarta.validation.Valid;

// F1 er endpoints. role check SecurityConfig e hoy, ekhane sudhu service ke call dey
@RestController
@RequestMapping("/api")
public class SupervisorCapacityController {

    private final SupervisorCapacityService service;

    public SupervisorCapacityController(SupervisorCapacityService service) {
        this.service = service;
    }

    // nijer profile ar capacity dekha, jemon ?semester=Fall 2026
    @GetMapping("/supervisor/me/capacity")
    public SupervisorCapacityView myCapacity(Authentication auth, @RequestParam String semester) {
        return service.getMyCapacity(auth.getName(), semester);
    }

    // nijer profile update kora
    @PutMapping("/supervisor/me/profile")
    public SupervisorCapacityView updateProfile(Authentication auth,
                                                @RequestParam String semester,
                                                @Valid @RequestBody UpdateProfileRequest req) {
        return service.updateProfile(auth.getName(), semester, req);
    }

    // capacity set kora, purano version dile 409 dey
    @PutMapping("/supervisor/me/capacity")
    public SupervisorCapacityView updateCapacity(Authentication auth,
                                                 @Valid @RequestBody UpdateCapacityRequest req) {
        return service.updateCapacity(auth.getName(), req);
    }

    // coordinator er override, reason soho audit row o save hoy
    @PutMapping("/coordinator/supervisors/{profileId}/capacity")
    public SupervisorCapacityView overrideCapacity(@PathVariable Long profileId,
                                                   Authentication auth,
                                                   @Valid @RequestBody OverrideCapacityRequest req) {
        return service.overrideCapacity(profileId, auth.getName(), req);
    }

    // coordinator override history dekhe, notun ta age ashe
    @GetMapping("/coordinator/supervisors/{profileId}/overrides")
    public List<CapacityOverrideView> overrides(@PathVariable Long profileId) {
        return service.getOverrides(profileId);
    }
}
