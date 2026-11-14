package com.thesismate.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thesismate.model.GroupMember;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    Optional<GroupMember> findByMemberIdAndActiveTrue(Long memberId);
    boolean existsByMemberIdAndActiveTrue(Long memberId);
    long countByGroupIdAndActiveTrue(Long groupId);
    List<GroupMember> findByGroupIdAndActiveTrue(Long groupId);
}