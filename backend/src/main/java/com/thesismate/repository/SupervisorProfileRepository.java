package com.thesismate.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import com.thesismate.model.SupervisorProfile;

public interface SupervisorProfileRepository extends JpaRepository<SupervisorProfile, Long> {

    @Override
    @EntityGraph(attributePaths = {"user", "researchAreas", "tracks"})
    List<SupervisorProfile> findAll();

    Optional<SupervisorProfile> findByUserId(Long userId);
    Optional<SupervisorProfile> findByUserEmail(String email);
}