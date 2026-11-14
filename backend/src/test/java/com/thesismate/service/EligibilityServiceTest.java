package com.thesismate.service;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.thesismate.model.StudentRecord;
import com.thesismate.model.Track;

class EligibilityServiceTest {

    private final EligibilityService service = new EligibilityService();

    private StudentRecord record(int credits, String cgpa) {
        return new StudentRecord("20000001", credits, new BigDecimal(cgpa), 1L);
    }

    @Test
    void eligibleAtExactBoundariesForThesis() {
        assertTrue(service.checkStudent(record(75, "3.00"), Track.THESIS).isEmpty());
    }

    @Test
    void rejectsFewerThan75Credits() {
        assertEquals(1, service.checkStudent(record(74, "3.80"), Track.THESIS).size());
    }

    @Test
    void thesisNeedsCgpaOfThree() {
        assertFalse(service.checkStudent(record(100, "2.99"), Track.THESIS).isEmpty());
    }

    @Test
    void projectAndInternshipNeedCgpaOfTwo() {
        assertTrue(service.checkStudent(record(75, "2.00"), Track.PROJECT).isEmpty());
        assertTrue(service.checkStudent(record(75, "2.00"), Track.INTERNSHIP).isEmpty());
        assertFalse(service.checkStudent(record(75, "1.99"), Track.PROJECT).isEmpty());
    }

    @Test
    void reportsBothProblemsAtOnce() {
        assertEquals(2, service.checkStudent(record(60, "1.50"), Track.THESIS).size());
    }

    @Test
    void missingRecordIsNotEligible() {
        assertEquals(1, service.checkStudent(null, Track.THESIS).size());
    }

    @Test
    void internshipGroupHasExactlyOneMember() {
        assertEquals(1, service.maxGroupSize(Track.INTERNSHIP));
        assertTrue(service.canAddMember(Track.INTERNSHIP, 0));
        assertFalse(service.canAddMember(Track.INTERNSHIP, 1));
    }

    @Test
    void otherTracksAllowUpToFiveMembers() {
        assertTrue(service.canAddMember(Track.THESIS, 4));
        assertFalse(service.canAddMember(Track.THESIS, 5));
        assertTrue(service.canAddMember(Track.PROJECT, 4));
        assertFalse(service.canAddMember(Track.PROJECT, 5));
    }
}