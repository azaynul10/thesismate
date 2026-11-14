package com.thesismate.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thesismate.model.GroupInvitation;

public interface GroupInvitationRepository extends JpaRepository<GroupInvitation, Long> {
    List<GroupInvitation> findByInviteeIdAndStatus(Long inviteeId, GroupInvitation.Status status);
    List<GroupInvitation> findByGroupId(Long groupId);
    boolean existsByGroupIdAndInviteeIdAndStatus(Long groupId, Long inviteeId, GroupInvitation.Status status);
}