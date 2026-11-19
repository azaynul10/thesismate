package com.thesismate.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thesismate.model.Phase;
import com.thesismate.model.SubmissionWindow;
import com.thesismate.model.Track;

public interface SubmissionWindowRepository extends JpaRepository<SubmissionWindow, Long> {

    Optional<SubmissionWindow> findBySemesterAndPhaseAndTrack(String semester, Phase phase, Track track);

    boolean existsBySemesterAndPhaseAndTrack(String semester, Phase phase, Track track);

    List<SubmissionWindow> findAllByOrderBySemesterDescPhaseAscTrackAsc();
}
