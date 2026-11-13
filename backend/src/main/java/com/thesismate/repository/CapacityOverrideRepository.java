package com.thesismate.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.thesismate.model.CapacityOverride;

public interface CapacityOverrideRepository extends JpaRepository<CapacityOverride, Long> {

    List<CapacityOverride> findByProfileIdOrderByCreatedAtDesc(Long profileId);
}