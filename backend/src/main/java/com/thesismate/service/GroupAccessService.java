package com.thesismate.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.thesismate.model.Allocation;
import com.thesismate.model.AppUser;
import com.thesismate.model.Role;
import com.thesismate.model.StudentGroup;
import com.thesismate.repository.AllocationRepository;
import com.thesismate.repository.AppUserRepository;
import com.thesismate.repository.GroupMemberRepository;
import com.thesismate.repository.StudentGroupRepository;

/**
 * Who may see or change the data of one group. F16 and F17 use it, and the later group-based features
 * (F12 review, F18 meetings, F19 evaluation, F24 acceptance tests) can use the same three checks:
 * a member of the group, a supervisor allocated to the group (F7), or the coordinator.
 */
@Service
public class GroupAccessService {

    public enum Viewer { MEMBER, SUPERVISOR, COORDINATOR }

    /** The resolved user and group, and in which capacity the user may use the group's data. */
    public record Access(AppUser user, StudentGroup group, Viewer viewer) {

        public boolean isMember() {
            return viewer == Viewer.MEMBER;
        }
    }

    private final AppUserRepository users;
    private final StudentGroupRepository groups;
    private final GroupMemberRepository members;
    private final AllocationRepository allocations;

    public GroupAccessService(AppUserRepository users, StudentGroupRepository groups,
                              GroupMemberRepository members, AllocationRepository allocations) {
        this.users = users;
        this.groups = groups;
        this.members = members;
        this.allocations = allocations;
    }

    public AppUser requireUser(Long userId) {
        return users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in"));
    }

    public StudentGroup requireGroup(Long groupId) {
        return groups.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
    }

    public boolean isActiveMember(Long userId, Long groupId) {
        return members.findByGroupIdAndActiveTrue(groupId).stream()
                .anyMatch(m -> m.getMember().getId().equals(userId));
    }

    /** Members, allocated supervisors and the coordinator may read. Everyone else gets 403. */
    public Access requireReadAccess(Long userId, Long groupId) {
        AppUser user = requireUser(userId);
        StudentGroup group = requireGroup(groupId);
        if (isActiveMember(userId, groupId)) {
            return new Access(user, group, Viewer.MEMBER);
        }
        if (user.getRole() == Role.SUPERVISOR && isAllocatedSupervisor(userId, groupId)) {
            return new Access(user, group, Viewer.SUPERVISOR);
        }
        if (user.getRole() == Role.COORDINATOR) {
            return new Access(user, group, Viewer.COORDINATOR);
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this group");
    }

    public Access requireMember(Long userId, Long groupId) {
        AppUser user = requireUser(userId);
        StudentGroup group = requireGroup(groupId);
        if (!isActiveMember(userId, groupId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this group");
        }
        return new Access(user, group, Viewer.MEMBER);
    }

    /** A supervisor or co-supervisor with an active allocation to the group. */
    public Access requireAllocatedSupervisor(Long userId, Long groupId) {
        AppUser user = requireUser(userId);
        StudentGroup group = requireGroup(groupId);
        if (user.getRole() != Role.SUPERVISOR || !isAllocatedSupervisor(userId, groupId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a supervisor of this group");
        }
        return new Access(user, group, Viewer.SUPERVISOR);
    }

    /** Changes are only allowed while the group is active. */
    public void requireActive(StudentGroup group) {
        if (group.getStatus() != StudentGroup.Status.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This group is no longer active");
        }
    }

    private boolean isAllocatedSupervisor(Long userId, Long groupId) {
        return allocations.existsByGroupIdAndSupervisorIdAndStatus(groupId, userId, Allocation.Status.ACTIVE);
    }
}
