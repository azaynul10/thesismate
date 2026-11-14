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

import com.thesismate.dto.CreateGroupRequest;
import com.thesismate.dto.GroupResponse;
import com.thesismate.dto.InvitationResponse;
import com.thesismate.dto.InviteRequest;
import com.thesismate.dto.TransferLeaderRequest;
import com.thesismate.repository.AppUserRepository;
import com.thesismate.service.GroupService;

import jakarta.validation.Valid;

/** F6: student-facing group endpoints. The rules live in GroupService. */
@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;
    private final AppUserRepository users;

    public GroupController(GroupService groupService, AppUserRepository users) {
        this.groupService = groupService;
        this.users = users;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse create(@Valid @RequestBody CreateGroupRequest request, Authentication auth) {
        return groupService.createGroup(currentUserId(auth), request.track(), request.semester());
    }

    @GetMapping("/me")
    public GroupResponse myGroup(Authentication auth) {
        return groupService.getMyGroup(currentUserId(auth))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "You are not in a group yet"));
    }

    @PostMapping("/{groupId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public InvitationResponse invite(@PathVariable Long groupId,
                                     @Valid @RequestBody InviteRequest request, Authentication auth) {
        return groupService.inviteByStudentId(currentUserId(auth), groupId, request.studentId());
    }

    @GetMapping("/invitations/me")
    public List<InvitationResponse> myInvitations(Authentication auth) {
        return groupService.getMyInvitations(currentUserId(auth));
    }

    @PostMapping("/invitations/{invitationId}/accept")
    public InvitationResponse accept(@PathVariable Long invitationId, Authentication auth) {
        return groupService.respondToInvitation(currentUserId(auth), invitationId, true);
    }

    @PostMapping("/invitations/{invitationId}/decline")
    public InvitationResponse decline(@PathVariable Long invitationId, Authentication auth) {
        return groupService.respondToInvitation(currentUserId(auth), invitationId, false);
    }

    @PostMapping("/{groupId}/leader")
    public GroupResponse transferLeader(@PathVariable Long groupId,
                                        @Valid @RequestBody TransferLeaderRequest request, Authentication auth) {
        return groupService.transferLeader(currentUserId(auth), groupId, request.studentId());
    }

    private Long currentUserId(Authentication auth) {
        return users.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in"))
                .getId();
    }
}
