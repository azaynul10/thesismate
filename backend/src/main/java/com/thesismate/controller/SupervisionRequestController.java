package com.thesismate.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.thesismate.dto.AllocationResponse;
import com.thesismate.dto.CreateSupervisionRequest;
import com.thesismate.dto.RejectRequest;
import com.thesismate.dto.SupervisionRequestResponse;
import com.thesismate.repository.AppUserRepository;
import com.thesismate.service.SupervisionRequestService;

import jakarta.validation.Valid;

/** F7: pitches from groups and the supervisor's answers. The rules live in SupervisionRequestService. */
@RestController
@RequestMapping("/api")
public class SupervisionRequestController {

    private final SupervisionRequestService service;
    private final AppUserRepository users;

    public SupervisionRequestController(SupervisionRequestService service, AppUserRepository users) {
        this.service = service;
        this.users = users;
    }

    // ---- group side ----

    @PostMapping("/groups/{groupId}/supervision-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public SupervisionRequestResponse send(@PathVariable Long groupId,
                                           @Valid @RequestBody CreateSupervisionRequest request,
                                           Authentication auth) {
        return service.sendRequest(currentUserId(auth), groupId, request.supervisorId(),
                request.role(), request.pitch());
    }

    @GetMapping("/groups/{groupId}/supervision-requests")
    public List<SupervisionRequestResponse> groupRequests(@PathVariable Long groupId, Authentication auth) {
        return service.getGroupRequests(currentUserId(auth), groupId);
    }

    @GetMapping("/groups/{groupId}/supervisors")
    public List<AllocationResponse> groupSupervisors(@PathVariable Long groupId, Authentication auth) {
        return service.getGroupAllocations(currentUserId(auth), groupId);
    }

    @PostMapping("/supervision-requests/{requestId}/withdraw")
    public SupervisionRequestResponse withdraw(@PathVariable Long requestId, Authentication auth) {
        return service.withdraw(currentUserId(auth), requestId);
    }

    // ---- supervisor side (SecurityConfig limits /api/supervisor/** to the SUPERVISOR role) ----

    @GetMapping("/supervisor/requests")
    public List<SupervisionRequestResponse> pendingRequests(Authentication auth) {
        return service.getPendingForSupervisor(currentUserId(auth));
    }

    @PostMapping("/supervisor/requests/{requestId}/accept")
    public SupervisionRequestResponse accept(@PathVariable Long requestId, Authentication auth) {
        return service.accept(currentUserId(auth), requestId);
    }

    @PostMapping("/supervisor/requests/{requestId}/reject")
    public SupervisionRequestResponse reject(@PathVariable Long requestId,
                                             @Valid @RequestBody RejectRequest request, Authentication auth) {
        return service.reject(currentUserId(auth), requestId, request.reason());
    }

    @GetMapping("/supervisor/groups")
    public List<AllocationResponse> myGroups(Authentication auth) {
        return service.getMyAllocations(currentUserId(auth));
    }

    private Long currentUserId(Authentication auth) {
        return users.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in"))
                .getId();
    }
}