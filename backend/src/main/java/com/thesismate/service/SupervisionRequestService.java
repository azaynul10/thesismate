package com.thesismate.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.thesismate.dto.AllocationResponse;
import com.thesismate.dto.SupervisionRequestResponse;
import com.thesismate.model.Allocation;
import com.thesismate.model.AppUser;
import com.thesismate.model.Role;
import com.thesismate.model.StudentGroup;
import com.thesismate.model.SupervisionRequest;
import com.thesismate.model.SupervisorRole;
import com.thesismate.repository.AllocationRepository;
import com.thesismate.repository.AppUserRepository;
import com.thesismate.repository.GroupMemberRepository;
import com.thesismate.repository.StudentGroupRepository;
import com.thesismate.repository.SupervisionRequestRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

/** F7: groups pitch supervisors; supervisors reject or accept. */
@Service
public class SupervisionRequestService {

    public static final int MAX_PENDING_REQUESTS = 3;
    public static final int MAX_PER_ROLE = 2;
    public static final int REQUEST_VALIDITY_DAYS = 7;

    private final AppUserRepository users;
    private final StudentGroupRepository groups;
    private final GroupMemberRepository members;
    private final SupervisionRequestRepository requests;
    private final AllocationRepository allocations;
    private final SupervisorCapacityLookup lookup;
    private final EntityManager entityManager;

    public SupervisionRequestService(AppUserRepository users,
                                     StudentGroupRepository groups,
                                     GroupMemberRepository members,
                                     SupervisionRequestRepository requests,
                                     AllocationRepository allocations,
                                     SupervisorCapacityLookup lookup,
                                     EntityManager entityManager) {
        this.users = users;
        this.groups = groups;
        this.members = members;
        this.requests = requests;
        this.allocations = allocations;
        this.lookup = lookup;
        this.entityManager = entityManager;;
    }

    @Transactional
    public SupervisionRequestResponse sendRequest(Long userId, Long groupId, Long supervisorId,
                                                  SupervisorRole role, String pitch) {
        // Lock the group so two clicks at once cannot slip past the "3 pending" limit.
        StudentGroup group = groups.findByIdForUpdate(groupId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Group not found"));
        if (group.getStatus() != StudentGroup.Status.ACTIVE) {
            throw error(HttpStatus.CONFLICT, "This group is no longer active");
        }
        if (!group.getLeader().getId().equals(userId)) {
            throw error(HttpStatus.FORBIDDEN, "Only the group leader can send supervision requests");
        }
        if (supervisorId == null || role == null || pitch == null || pitch.isBlank()) {
            throw error(HttpStatus.BAD_REQUEST, "Supervisor, role and pitch are required");
        }
        AppUser supervisor = users.findById(supervisorId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Supervisor not found"));
        if (supervisor.getRole() != Role.SUPERVISOR) {
            throw error(HttpStatus.BAD_REQUEST, "That user is not a supervisor");
        }
        if (!lookup.tracksFor(supervisorId).contains(group.getTrack())) {
            throw error(HttpStatus.BAD_REQUEST, "This supervisor does not supervise the "
                    + group.getTrack() + " track");
        }
        if (allocations.existsByGroupIdAndSupervisorIdAndStatus(groupId, supervisorId, Allocation.Status.ACTIVE)) {
            throw error(HttpStatus.CONFLICT, "This supervisor is already assigned to your group");
        }
        if (requests.existsByGroupIdAndSupervisorIdAndStatus(groupId, supervisorId, SupervisionRequest.Status.PENDING)) {
            throw error(HttpStatus.CONFLICT, "You already have a pending request to this supervisor");
        }
        if (requests.countByGroupIdAndStatus(groupId, SupervisionRequest.Status.PENDING) >= MAX_PENDING_REQUESTS) {
            throw error(HttpStatus.CONFLICT, "A group can have at most " + MAX_PENDING_REQUESTS
                    + " pending requests");
        }
        if (allocations.countByGroupIdAndSupervisorRoleAndStatus(groupId, role, Allocation.Status.ACTIVE)
                >= MAX_PER_ROLE) {
            throw error(HttpStatus.CONFLICT, "Your group already has " + MAX_PER_ROLE + " "
                    + roleLabel(role) + "s");
        }
        long used = allocations.countBySupervisorIdAndSemesterAndStatus(
                supervisorId, group.getSemester(), Allocation.Status.ACTIVE);
        if (used >= lookup.capacityFor(supervisorId, group.getSemester())) {
            throw error(HttpStatus.CONFLICT, "This supervisor has no free slots this semester");
        }

        SupervisionRequest saved = requests.save(new SupervisionRequest(
                group, supervisor, role, pitch.trim(), group.getLeader(),
                LocalDateTime.now().plusDays(REQUEST_VALIDITY_DAYS)));
        return toResponse(saved);
    }

    @Transactional
    public SupervisionRequestResponse withdraw(Long userId, Long requestId) {
        SupervisionRequest request = find(requestId);
        if (!request.getGroup().getLeader().getId().equals(userId)) {
            throw error(HttpStatus.FORBIDDEN, "Only the group leader can withdraw a request");
        }
        requirePending(request);
        request.withdraw();
        return toResponse(request);
    }

    @Transactional
    public SupervisionRequestResponse reject(Long userId, Long requestId, String reason) {
        SupervisionRequest request = find(requestId);
        if (!request.getSupervisor().getId().equals(userId)) {
            throw error(HttpStatus.FORBIDDEN, "This request is not addressed to you");
        }
        if (reason == null || reason.isBlank()) {
            throw error(HttpStatus.BAD_REQUEST, "A reason is required to reject a request");
        }
        requirePending(request);
        requireNotExpired(request);
        request.reject(reason);
        return toResponse(request);
    }

    @Transactional(readOnly = true)
    public List<SupervisionRequestResponse> getGroupRequests(Long userId, Long groupId) {
        boolean isMember = members.findByGroupIdAndActiveTrue(groupId).stream()
                .anyMatch(m -> m.getMember().getId().equals(userId));
        if (!isMember) {
            throw error(HttpStatus.FORBIDDEN, "You are not a member of this group");
        }
        return requests.findByGroupIdOrderByCreatedAtDesc(groupId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SupervisionRequestResponse> getPendingForSupervisor(Long supervisorId) {
        LocalDateTime now = LocalDateTime.now();
        return requests.findBySupervisorIdAndStatusOrderByCreatedAtAsc(
                        supervisorId, SupervisionRequest.Status.PENDING).stream()
                .filter(r -> r.getExpiresAt().isAfter(now))
                .map(this::toResponse)
                .toList();
    }

    /**
     * Accepting runs in one transaction: lock the supervisor and the group, re-check every limit,
     * create the allocation, and close the group's other requests for a role that is now full.
     */
    @Transactional
    public SupervisionRequestResponse accept(Long userId, Long requestId) {
        SupervisionRequest request = find(requestId);
        if (!request.getSupervisor().getId().equals(userId)) {
            throw error(HttpStatus.FORBIDDEN, "This request is not addressed to you");
        }

        // Lock order is always supervisor first, then group, so two acceptances cannot deadlock.
        entityManager.find(AppUser.class, userId, LockModeType.PESSIMISTIC_WRITE);
        StudentGroup group = groups.findByIdForUpdate(request.getGroup().getId())
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Group not found"));
        // The request may have been withdrawn while we waited for the locks.
        entityManager.refresh(request);

        requirePending(request);
        requireNotExpired(request);
        if (group.getStatus() != StudentGroup.Status.ACTIVE) {
            throw error(HttpStatus.CONFLICT, "This group is no longer active");
        }
        SupervisorRole role = request.getSupervisorRole();
        if (!lookup.tracksFor(userId).contains(group.getTrack())) {
            throw error(HttpStatus.BAD_REQUEST, "You do not supervise the " + group.getTrack() + " track");
        }
        if (allocations.existsByGroupIdAndSupervisorIdAndStatus(group.getId(), userId, Allocation.Status.ACTIVE)) {
            throw error(HttpStatus.CONFLICT, "You are already assigned to this group");
        }
        long roleCount = allocations.countByGroupIdAndSupervisorRoleAndStatus(
                group.getId(), role, Allocation.Status.ACTIVE);
        if (roleCount >= MAX_PER_ROLE) {
            throw error(HttpStatus.CONFLICT, "This group already has " + MAX_PER_ROLE + " "
                    + roleLabel(role) + "s");
        }
        long used = allocations.countBySupervisorIdAndSemesterAndStatus(
                userId, group.getSemester(), Allocation.Status.ACTIVE);
        if (used >= lookup.capacityFor(userId, group.getSemester())) {
            throw error(HttpStatus.CONFLICT, "You have no free slots this semester");
        }

        allocations.save(new Allocation(group, request.getSupervisor(), role, group.getSemester(), request));
        request.accept();

        if (roleCount + 1 >= MAX_PER_ROLE) {
            for (SupervisionRequest other : requests.findByGroupIdAndStatus(
                    group.getId(), SupervisionRequest.Status.PENDING)) {
                if (!other.getId().equals(request.getId()) && other.getSupervisorRole() == role) {
                    other.withdraw();
                }
            }
        }
        return toResponse(request);
    }

    @Transactional(readOnly = true)
    public List<AllocationResponse> getGroupAllocations(Long userId, Long groupId) {
        boolean isMember = members.findByGroupIdAndActiveTrue(groupId).stream()
                .anyMatch(m -> m.getMember().getId().equals(userId));
        if (!isMember) {
            throw error(HttpStatus.FORBIDDEN, "You are not a member of this group");
        }
        return allocations.findByGroupIdAndStatus(groupId, Allocation.Status.ACTIVE).stream()
                .map(this::toAllocationResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AllocationResponse> getMyAllocations(Long supervisorId) {
        return allocations.findBySupervisorIdAndStatus(supervisorId, Allocation.Status.ACTIVE).stream()
                .map(this::toAllocationResponse)
                .toList();
    }

    // ---- helpers ----

    private SupervisionRequest find(Long requestId) {
        return requests.findById(requestId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Request not found"));
    }

    private void requirePending(SupervisionRequest request) {
        if (request.getStatus() != SupervisionRequest.Status.PENDING) {
            throw error(HttpStatus.CONFLICT, "This request is already "
                    + request.getStatus().name().toLowerCase());
        }
    }

    private void requireNotExpired(SupervisionRequest request) {
        if (!request.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw error(HttpStatus.CONFLICT, "This request has expired");
        }
    }

    private String roleLabel(SupervisorRole role) {
        return role == SupervisorRole.CO_SUPERVISOR ? "co-supervisor" : "supervisor";
    }

    private SupervisionRequestResponse toResponse(SupervisionRequest r) {
        List<String> memberNames = members.findByGroupIdAndActiveTrue(r.getGroup().getId()).stream()
                .map(m -> m.getMember().getFullName() + " (" + m.getMember().getStudentId() + ")")
                .toList();
        return new SupervisionRequestResponse(
                r.getId(), r.getGroup().getId(), r.getGroup().getTrack(), r.getGroup().getSemester(),
                r.getSupervisor().getId(), r.getSupervisor().getFullName(), r.getSupervisorRole(),
                r.getPitch(), r.getStatus().name(), r.getRejectionReason(), memberNames,
                r.getCreatedAt(), r.getExpiresAt(), r.getRespondedAt());
    }

    private AllocationResponse toAllocationResponse(Allocation a) {
        List<String> memberNames = members.findByGroupIdAndActiveTrue(a.getGroup().getId()).stream()
                .map(m -> m.getMember().getFullName() + " (" + m.getMember().getStudentId() + ")")
                .toList();
        return new AllocationResponse(
                a.getId(), a.getGroup().getId(), a.getGroup().getTrack(), a.getSemester(),
                a.getSupervisor().getId(), a.getSupervisor().getFullName(), a.getSupervisorRole(),
                memberNames, a.getCreatedAt());
    }

    private ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}