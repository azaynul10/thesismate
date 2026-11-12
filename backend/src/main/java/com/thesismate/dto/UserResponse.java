package com.thesismate.dto;

import com.thesismate.model.AppUser;
import com.thesismate.model.Role;

public record UserResponse(Long id, String email, String fullName, Role role, String studentId) {
    public static UserResponse from(AppUser u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getFullName(), u.getRole(), u.getStudentId());
    }
}