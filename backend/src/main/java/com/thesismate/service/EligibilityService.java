package com.thesismate.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.thesismate.model.StudentRecord;
import com.thesismate.model.Track;

/** F6 rules from the department policy: credits, CGPA and group size. */
@Service
public class EligibilityService {

    public static final int MIN_CREDITS = 75;
    public static final BigDecimal MIN_CGPA_THESIS = new BigDecimal("3.00");
    public static final BigDecimal MIN_CGPA_PROJECT_OR_INTERNSHIP = new BigDecimal("2.00");
    public static final int MAX_GROUP_SIZE = 5;

    /** Returns the reasons the student cannot join a group on this track. Empty list = eligible. */
    public List<String> checkStudent(StudentRecord record, Track track) {
        if (record == null) {
            return List.of("No student record found. Ask the coordinator to import the records.");
        }
        List<String> problems = new ArrayList<>();
        if (record.getCreditsCompleted() < MIN_CREDITS) {
            problems.add("At least " + MIN_CREDITS + " completed credits are required (has "
                    + record.getCreditsCompleted() + ")");
        }
        BigDecimal required = track == Track.THESIS ? MIN_CGPA_THESIS : MIN_CGPA_PROJECT_OR_INTERNSHIP;
        if (record.getCgpa().compareTo(required) < 0) {
            problems.add("A CGPA of at least " + required + " is required for " + track
                    + " (has " + record.getCgpa() + ")");
        }
        return problems;
    }

    /** Internship groups have exactly one student, the other tracks up to five. */
    public int maxGroupSize(Track track) {
        return track == Track.INTERNSHIP ? 1 : MAX_GROUP_SIZE;
    }

    public boolean canAddMember(Track track, long currentSize) {
        return currentSize < maxGroupSize(track);
    }
}