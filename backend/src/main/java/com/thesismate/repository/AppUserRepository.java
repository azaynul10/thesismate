package com.thesismate.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thesismate.model.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);
    Optional<AppUser> findByStudentId(String studentId);
    boolean existsByEmail(String email);
    boolean existsByStudentId(String studentId);
}