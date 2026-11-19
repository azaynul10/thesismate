package com.thesismate.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.thesismate.dto.SupervisorDirectoryItem;
import com.thesismate.dto.SupervisorPageResponse;
import com.thesismate.model.Track;
import com.thesismate.service.SupervisorDiscoveryService;

/** F2: public supervisor discovery endpoints for logged-in students, supervisors, and coordinators. */
@RestController
@RequestMapping("/api/supervisors")
public class SupervisorDiscoveryController {

    private final SupervisorDiscoveryService service;

    public SupervisorDiscoveryController(SupervisorDiscoveryService service) {
        this.service = service;
    }

    @GetMapping
    public SupervisorPageResponse search(
            @RequestParam String semester,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Track track,
            @RequestParam(required = false) Boolean hasSlots,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String effectiveKeyword = (keyword != null && !keyword.isBlank()) ? keyword : q;
        return service.search(semester, area, effectiveKeyword, track, hasSlots, sort, page, size);
    }

    @GetMapping("/{profileId}")
    public SupervisorDirectoryItem getById(
            @PathVariable Long profileId,
            @RequestParam String semester) {
        return service.getByProfileId(profileId, semester);
    }
}
