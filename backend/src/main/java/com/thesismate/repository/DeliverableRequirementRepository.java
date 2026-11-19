package com.thesismate.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thesismate.model.DeliverableRequirement;
import com.thesismate.model.DeliverableType;
import com.thesismate.model.Phase;
import com.thesismate.model.Track;

public interface DeliverableRequirementRepository extends JpaRepository<DeliverableRequirement, Long> {

    List<DeliverableRequirement> findByPhaseAndTrack(Phase phase, Track track);

    Optional<DeliverableRequirement> findByPhaseAndTrackAndDeliverableType(
            Phase phase, Track track, DeliverableType deliverableType);

    List<DeliverableRequirement> findAllByOrderByPhaseAscTrackAscDeliverableTypeAsc();
}
