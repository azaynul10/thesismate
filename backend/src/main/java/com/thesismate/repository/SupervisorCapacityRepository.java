package com.thesismate.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.thesismate.model.SupervisorCapacity;

public interface SupervisorCapacityRepository extends JpaRepository<SupervisorCapacity, Long> {

    Optional<SupervisorCapacity> findByProfileIdAndSemester(Long profileId, String semester);
}