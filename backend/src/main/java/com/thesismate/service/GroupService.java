package com.thesismate.service;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.thesismate.dto.GroupResponse;
import com.thesismate.dto.InvitationResponse;
import com.thesismate.model.AppUser;
import com.thesismate.model.GroupInvitation;
import com.thesismate.model.GroupMember;
import com.thesismate.model.Role;
import com.thesismate.model.StudentGroup;
import com.thesismate.model.Track;
import com.thesismate.repository.AppUserRepository;
import com.thesismate.repository.GroupInvitationRepository;
import com.thesismate.repository.GroupMemberRepository;
import com.thesismate.repository.StudentGroupRepository;
import com.thesismate.repository.StudentRecordRepository;

/** F6: group formation. Rules come from the feature document. */
@Service
public class GroupService {

    private final AppUserRepository users;
    private final StudentRecordRepository records;
    private final StudentGroupRepository groups;
    private final GroupMemberRepository members;
    private final GroupInvitationRepository invitations;
    private final EligibilityService eligibility;

    public GroupService(AppUserRepository users,
                        StudentRecordRepository records,
                        StudentGroupRepository groups,
                        GroupMemberRepository members,
                        GroupInvitationRepository invitations,
                        EligibilityService eligibility) {
        this.users = users;
        this.records = records;
        this.groups = groups;
        this.members = members;
        this.invitations = invitations;
        this.eligibility = eligibility;
    }

    @Transactional
    public GroupResponse createGroup(Long userId, Track track, String semester) {
        AppUser user = requireStudent(userId);
        if (track == null || semester == null || semester.isBlank()) {
            throw error(HttpStatus.BAD_REQUEST, "Track and semester are required");
        }
        if (members.existsByMemberIdAndActiveTrue(userId)) {
            throw error(HttpStatus.CONFLICT, "You are already in an active group");
        }
        requireEligible(user, track, "You are not eligible");
        StudentGroup group = groups.save(new StudentGroup(track, semester.trim(), user));
        members.save(new GroupMember(group, user));
        return toResponse(group);
    }

    @Transactional
    public InvitationResponse inviteByStudentId(Long userId, Long groupId, String studentId) {
        StudentGroup group = groups.findByIdForUpdate(groupId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Group not found"));
        if (group.getStatus() != StudentGroup.Status.ACTIVE) {
            throw error(HttpStatus.CONFLICT, "This group is no longer active");
        }
        if (!group.getLeader().getId().equals(userId)) {
            throw error(HttpStatus.FORBIDDEN, "Only the group leader can invite members");
        }
        AppUser invitee = users.findByStudentId(studentId == null ? "" : studentId.trim())
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "No student with that ID"));
        if (invitee.getRole() != Role.STUDENT) {
            throw error(HttpStatus.BAD_REQUEST, "That account is not a student");
        }
        if (invitee.getId().equals(userId)) {
            throw error(HttpStatus.BAD_REQUEST, "You are already in this group");
        }
        if (members.existsByMemberIdAndActiveTrue(invitee.getId())) {
            throw error(HttpStatus.CONFLICT, "That student is already in an active group");
        }
        requireCapacity(group);
        requireEligible(invitee, group.getTrack(), "That student is not eligible");
        if (invitations.existsByGroupIdAndInviteeIdAndStatus(
                groupId, invitee.getId(), GroupInvitation.Status.PENDING)) {
            throw error(HttpStatus.CONFLICT, "That student already has a pending invitation");
        }
        AppUser leader = users.findById(userId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "User not found"));
        GroupInvitation saved = invitations.save(new GroupInvitation(group, invitee, leader));
        return toResponse(saved);
    }

    @Transactional
    public InvitationResponse respondToInvitation(Long userId, Long invitationId, boolean accept) {
        GroupInvitation invitation = invitations.findById(invitationId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Invitation not found"));
        if (!invitation.getInvitee().getId().equals(userId)) {
            throw error(HttpStatus.FORBIDDEN, "This invitation is not addressed to you");
        }
        if (invitation.getStatus() != GroupInvitation.Status.PENDING) {
            throw error(HttpStatus.CONFLICT, "This invitation has already been answered");
        }
        if (!accept) {
            invitation.respond(GroupInvitation.Status.DECLINED);
            return toResponse(invitation);
        }

        StudentGroup group = groups.findByIdForUpdate(invitation.getGroup().getId())
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Group not found"));
        if (group.getStatus() != StudentGroup.Status.ACTIVE) {
            throw error(HttpStatus.CONFLICT, "This group is no longer active");
        }
        if (members.existsByMemberIdAndActiveTrue(userId)) {
            throw error(HttpStatus.CONFLICT, "You are already in an active group");
        }
        requireCapacity(group);
        requireEligible(invitation.getInvitee(), group.getTrack(), "You are not eligible");

        members.save(new GroupMember(group, invitation.getInvitee()));
        invitation.respond(GroupInvitation.Status.ACCEPTED);

        for (GroupInvitation other : invitations.findByInviteeIdAndStatus(userId, GroupInvitation.Status.PENDING)) {
            if (!other.getId().equals(invitation.getId())) {
                other.respond(GroupInvitation.Status.CANCELLED);
            }
        }
        return toResponse(invitation);
    }

    @Transactional(readOnly = true)
    public Optional<GroupResponse> getMyGroup(Long userId) {
        return members.findByMemberIdAndActiveTrue(userId)
                .map(m -> toResponse(m.getGroup()));
    }

    @Transactional(readOnly = true)
    public List<InvitationResponse> getMyInvitations(Long userId) {
        return invitations.findByInviteeIdAndStatus(userId, GroupInvitation.Status.PENDING).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public GroupResponse transferLeader(Long userId, Long groupId, String studentId) {
        StudentGroup group = groups.findByIdForUpdate(groupId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Group not found"));
        if (group.getStatus() != StudentGroup.Status.ACTIVE) {
            throw error(HttpStatus.CONFLICT, "This group is no longer active");
        }
        if (!group.getLeader().getId().equals(userId)) {
            throw error(HttpStatus.FORBIDDEN, "Only the current leader can transfer leadership");
        }
        AppUser newLeader = users.findByStudentId(studentId == null ? "" : studentId.trim())
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "No student with that ID"));
        if (newLeader.getId().equals(userId)) {
            throw error(HttpStatus.BAD_REQUEST, "You are already the leader");
        }
        boolean isMember = members.findByGroupIdAndActiveTrue(groupId).stream()
                .anyMatch(m -> m.getMember().getId().equals(newLeader.getId()));
        if (!isMember) {
            throw error(HttpStatus.BAD_REQUEST, "That student is not a member of this group");
        }
        group.transferLeader(newLeader);
        return toResponse(group);
    }

    // ---- helpers ----

    private AppUser requireStudent(Long userId) {
        AppUser user = users.findById(userId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getRole() != Role.STUDENT) {
            throw error(HttpStatus.FORBIDDEN, "Only students can use groups");
        }
        return user;
    }

    private void requireEligible(AppUser student, Track track, String prefix) {
        List<String> problems = eligibility.checkStudent(
                records.findByStudentId(student.getStudentId()).orElse(null), track);
        if (!problems.isEmpty()) {
            throw error(HttpStatus.BAD_REQUEST, prefix + ": " + String.join("; ", problems));
        }
    }

    private void requireCapacity(StudentGroup group) {
        long size = members.countByGroupIdAndActiveTrue(group.getId());
        if (!eligibility.canAddMember(group.getTrack(), size)) {
            throw error(HttpStatus.CONFLICT, "This group cannot take more members (limit "
                    + eligibility.maxGroupSize(group.getTrack()) + " for " + group.getTrack() + ")");
        }
    }

    private GroupResponse toResponse(StudentGroup group) {
        List<GroupResponse.MemberInfo> list = members.findByGroupIdAndActiveTrue(group.getId()).stream()
                .map(m -> new GroupResponse.MemberInfo(
                        m.getMember().getId(),
                        m.getMember().getFullName(),
                        m.getMember().getStudentId(),
                        m.getMember().getId().equals(group.getLeader().getId())))
                .toList();
        return new GroupResponse(group.getId(), group.getTrack(), group.getSemester(),
                group.getStatus().name(), group.getLeader().getId(), group.getLeader().getFullName(), list);
    }

    private InvitationResponse toResponse(GroupInvitation i) {
        return new InvitationResponse(i.getId(), i.getGroup().getId(), i.getGroup().getTrack(),
                i.getGroup().getSemester(), i.getInvitedBy().getFullName(),
                i.getStatus().name(), i.getCreatedAt());
    }

    private ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}