package com.thesismate.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.thesismate.dto.MilestonePlanView;
import com.thesismate.dto.MilestoneRequest;
import com.thesismate.dto.MilestoneView;
import com.thesismate.dto.TaskRequest;
import com.thesismate.dto.TaskStatusRequest;
import com.thesismate.dto.TaskView;
import com.thesismate.repository.AppUserRepository;
import com.thesismate.service.MilestoneService;

import jakarta.validation.Valid;

/**
 * F16: milestone and task planning. The rules live in MilestoneService.
 * Every route needs a login. Who may see or change a group's plan is decided per group in the service.
 */
@RestController
@RequestMapping("/api")
public class MilestoneController {

    private final MilestoneService service;
    private final AppUserRepository users;

    public MilestoneController(MilestoneService service, AppUserRepository users) {
        this.service = service;
        this.users = users;
    }

    // ---- plan ----

    @GetMapping("/groups/{groupId}/milestones")
    public MilestonePlanView plan(@PathVariable Long groupId, Authentication auth) {
        return service.plan(currentUserId(auth), groupId);
    }

    /** Copies the milestone template of the group's track into its plan (once). */
    @PostMapping("/groups/{groupId}/milestones/template")
    @ResponseStatus(HttpStatus.CREATED)
    public MilestonePlanView applyTemplate(@PathVariable Long groupId, Authentication auth) {
        return service.applyTemplate(currentUserId(auth), groupId);
    }

    // ---- milestones ----

    @PostMapping("/groups/{groupId}/milestones")
    @ResponseStatus(HttpStatus.CREATED)
    public MilestoneView addMilestone(@PathVariable Long groupId, @Valid @RequestBody MilestoneRequest request,
                                      Authentication auth) {
        return service.addMilestone(currentUserId(auth), groupId, request);
    }

    @PutMapping("/milestones/{milestoneId}")
    public MilestoneView updateMilestone(@PathVariable Long milestoneId, @Valid @RequestBody MilestoneRequest request,
                                         Authentication auth) {
        return service.updateMilestone(currentUserId(auth), milestoneId, request);
    }

    @DeleteMapping("/milestones/{milestoneId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMilestone(@PathVariable Long milestoneId, Authentication auth) {
        service.deleteMilestone(currentUserId(auth), milestoneId);
    }

    // ---- tasks ----

    @PostMapping("/milestones/{milestoneId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskView addTask(@PathVariable Long milestoneId, @Valid @RequestBody TaskRequest request,
                            Authentication auth) {
        return service.addTask(currentUserId(auth), milestoneId, request);
    }

    @PutMapping("/tasks/{taskId}")
    public TaskView updateTask(@PathVariable Long taskId, @Valid @RequestBody TaskRequest request,
                               Authentication auth) {
        return service.updateTask(currentUserId(auth), taskId, request);
    }

    @PatchMapping("/tasks/{taskId}/status")
    public TaskView changeStatus(@PathVariable Long taskId, @Valid @RequestBody TaskStatusRequest request,
                                 Authentication auth) {
        return service.changeStatus(currentUserId(auth), taskId, request.status());
    }

    @DeleteMapping("/tasks/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable Long taskId, Authentication auth) {
        service.deleteTask(currentUserId(auth), taskId);
    }

    private Long currentUserId(Authentication auth) {
        return users.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in"))
                .getId();
    }
}
