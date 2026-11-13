package com.thesismate.dto;

import java.util.List;

import com.thesismate.model.Track;

public record GroupResponse(
        Long id,
        Track track,
        String semester,
        String status,
        Long leaderId,
        String leaderName,
        List<MemberInfo> members) {

    public record MemberInfo(Long userId, String fullName, String studentId, boolean leader) {
    }
}