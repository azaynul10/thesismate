package com.thesismate.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.thesismate.model.SupervisorProfile;

public interface SupervisorProfileRepository extends JpaRepository<SupervisorProfile, Long> {

    Optional<SupervisorProfile> findByUserId(Long userId);
    Optional<SupervisorProfile> findByUserEmail(String email);
}