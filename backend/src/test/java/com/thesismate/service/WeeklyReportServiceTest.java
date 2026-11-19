package com.thesismate.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.thesismate.dto.WeeklyOverviewView;
import com.thesismate.dto.WeeklyReportRequest;
import com.thesismate.dto.WeeklyReportView;
import com.thesismate.event.WeeklyReportReminderEvent;
import com.thesismate.event.WeeklyReportReviewedEvent;
import com.thesismate.event.WeeklyReportSubmittedEvent;
import com.thesismate.model.Allocation;
import com.thesismate.model.AppUser;
import com.thesismate.model.GroupMember;
import com.thesismate.model.LatePolicy;
import com.thesismate.model.Phase;
import com.thesismate.model.Role;
import com.thesismate.model.StudentGroup;
import com.thesismate.model.SubmissionWindow;
import com.thesismate.model.Track;
import com.thesismate.model.WeeklyReport;
import com.thesismate.model.WeeklyReportReminder;
import com.thesismate.model.WeeklyReportStatus;
import com.thesismate.repository.AllocationRepository;
import com.thesismate.repository.AppUserRepository;
import com.thesismate.repository.GroupMemberRepository;
import com.thesismate.repository.StudentGroupRepository;
import com.thesismate.repository.SubmissionWindowRepository;
import com.thesismate.repository.WeeklyReportReminderRepository;
import com.thesismate.repository.WeeklyReportRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WeeklyReportServiceTest {

    private static final String SEMESTER = "Spring 2027";
    /** Thursday 19 November 2026, so the current week starts on Monday 16 November. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-19T10:00:00Z"), ZoneId.of("UTC"));
    private static final LocalDate THIS_WEEK = LocalDate.of(2026, 11, 16);
    private static final LocalDate LAST_WEEK = LocalDate.of(2026, 11, 9);
    private static final LocalDate WEEK_BEFORE = LocalDate.of(2026, 11, 2);

    @Mock AppUserRepository users;
    @Mock StudentGroupRepository groups;
    @Mock GroupMemberRepository members;
    @Mock AllocationRepository allocations;
    @Mock WeeklyReportRepository reports;
    @Mock WeeklyReportReminderRepository reminders;
    @Mock SubmissionWindowRepository windows;
    @Mock ApplicationEventPublisher events;

    private WeeklyReportService service;
    private AppUser alice;
    private AppUser bob;
    private AppUser outsider;
    private AppUser supervisor;
    private AppUser otherSupervisor;
    private StudentGroup group;

    @BeforeEach
    void setUp() {
        GroupAccessService access = new GroupAccessService(users, groups, members, allocations);
        service = new WeeklyReportService(reports, reminders, members, allocations, windows, access, events, CLOCK);

        alice = person(1L, "20000001", Role.STUDENT);
        bob = person(2L, "20000002", Role.STUDENT);
        outsider = person(3L, "20000003", Role.STUDENT);
        supervisor = person(20L, null, Role.SUPERVISOR);
        otherSupervisor = person(21L, null, Role.SUPERVISOR);
        group = newGroup(10L, Track.THESIS);

        for (AppUser u : List.of(alice, bob, outsider, supervisor, otherSupervisor)) {
            when(users.findById(u.getId())).thenReturn(Optional.of(u));
        }
        when(groups.findById(10L)).thenReturn(Optional.of(group));
        when(members.findByGroupIdAndActiveTrue(10L)).thenReturn(List.of(
                new GroupMember(group, alice), new GroupMember(group, bob)));
        // The first supervisor was assigned on Monday 2 November, so reports are due from that week.
        when(reports.firstAllocationTime(10L)).thenReturn(LocalDateTime.of(2026, 11, 2, 9, 0));
        when(reports.findByGroup_IdOrderByWeekStartDescSubmittedAtDesc(10L)).thenReturn(List.of());
        when(reports.save(any(WeeklyReport.class))).thenAnswer(i -> i.getArgument(0));
        when(allocations.existsByGroupIdAndSupervisorIdAndStatus(10L, 20L, Allocation.Status.ACTIVE)).thenReturn(true);
    }

    private AppUser person(long id, String studentId, Role role) {
        AppUser u = new AppUser("u" + id + "@thesismate.test", "User " + id, role, "pw", studentId);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    private StudentGroup newGroup(long id, Track track) {
        StudentGroup g = new StudentGroup(track, SEMESTER, alice);
        ReflectionTestUtils.setField(g, "id", id);
        return g;
    }

    private WeeklyReport report(long id, LocalDate week) {
        WeeklyReport r = new WeeklyReport(group, week, "Did things", "Do more", null, alice,
                week.plusDays(3).atTime(10, 0));
        ReflectionTestUtils.setField(r, "id", id);
        when(reports.findById(id)).thenReturn(Optional.of(r));
        return r;
    }

    private WeeklyReportRequest request() {
        return new WeeklyReportRequest("Read three papers", "Write the survey", "Waiting for dataset access");
    }

    private void assertStatus(int expected, Executable action) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, action);
        assertEquals(expected, ex.getStatusCode().value());
    }

    // ---- submitting ----

    @Test
    void memberHandsInTheReportOfTheCurrentWeek() {
        WeeklyReportView view = service.submit(1L, 10L, THIS_WEEK, request());

        assertEquals(THIS_WEEK, view.weekStart());
        assertEquals(LocalDate.of(2026, 11, 22), view.weekEnd());
        assertEquals(WeeklyReportStatus.SUBMITTED, view.status());
        assertFalse(view.late());
        assertEquals("Waiting for dataset access", view.blockers());
        verify(events).publishEvent(any(WeeklyReportSubmittedEvent.class));
    }

    @Test
    void noWeekMeansTheCurrentWeek() {
        assertEquals(THIS_WEEK, service.submit(1L, 10L, null, request()).weekStart());
    }

    @Test
    void blockersAreOptional() {
        WeeklyReportView view = service.submit(1L, 10L, THIS_WEEK,
                new WeeklyReportRequest("Did X", "Will do Y", "   "));
        assertNull(view.blockers());
    }

    @Test
    void textIsTrimmedAndMustNotBeBlank() {
        assertStatus(400, () -> service.submit(1L, 10L, THIS_WEEK, new WeeklyReportRequest(" ", "next", null)));
        assertStatus(400, () -> service.submit(1L, 10L, THIS_WEEK, new WeeklyReportRequest("done", "", null)));
        assertEquals("Did X", service.submit(1L, 10L, THIS_WEEK, new WeeklyReportRequest("  Did X ", "Y", null)).done());
    }

    @Test
    void aMissedPastWeekCanBeHandedInLate() {
        WeeklyReportView view = service.submit(2L, 10L, LAST_WEEK, request());
        assertTrue(view.late());
        assertEquals(LAST_WEEK, view.weekStart());
    }

    @Test
    void onlyGroupMembersCanSubmit() {
        assertStatus(403, () -> service.submit(3L, 10L, THIS_WEEK, request()));
        assertStatus(403, () -> service.submit(20L, 10L, THIS_WEEK, request()));
    }

    @Test
    void internshipGroupsDoNotUseWeeklyReports() {
        StudentGroup internship = newGroup(11L, Track.INTERNSHIP);
        when(groups.findById(11L)).thenReturn(Optional.of(internship));
        when(members.findByGroupIdAndActiveTrue(11L)).thenReturn(List.of(new GroupMember(internship, alice)));

        assertStatus(400, () -> service.submit(1L, 11L, THIS_WEEK, request()));

        WeeklyOverviewView view = service.overview(1L, 11L);
        assertFalse(view.applicable());
        assertFalse(view.canSubmit());
        assertTrue(view.notice().contains("Internship"));
    }

    @Test
    void groupWithoutASupervisorCannotSubmitYet() {
        when(reports.firstAllocationTime(10L)).thenReturn(null);

        assertStatus(409, () -> service.submit(1L, 10L, THIS_WEEK, request()));

        WeeklyOverviewView view = service.overview(1L, 10L);
        assertTrue(view.applicable());
        assertFalse(view.canSubmit());
        assertNull(view.trackingStartsOn());
        assertTrue(view.missingWeeks().isEmpty());
    }

    @Test
    void futureWeeksAndNonMondaysAreRejected() {
        assertStatus(400, () -> service.submit(1L, 10L, LocalDate.of(2026, 11, 23), request()));
        assertStatus(400, () -> service.submit(1L, 10L, LocalDate.of(2026, 11, 17), request()));
    }

    @Test
    void weeksBeforeTheSupervisorWasAssignedAreRejected() {
        assertStatus(400, () -> service.submit(1L, 10L, LocalDate.of(2026, 10, 26), request()));
    }

    @Test
    void reportsStopAfterTheWeekTheFinalWindowCloses() {
        SubmissionWindow finalWindow = new SubmissionWindow(SEMESTER, Phase.FINAL, Track.THESIS,
                LocalDateTime.of(2026, 11, 1, 9, 0), LocalDateTime.of(2026, 11, 10, 23, 59),
                LatePolicy.NO_LATE, null, supervisor);
        when(windows.findBySemesterAndPhaseAndTrack(SEMESTER, Phase.FINAL, Track.THESIS))
                .thenReturn(Optional.of(finalWindow));

        assertStatus(400, () -> service.submit(1L, 10L, THIS_WEEK, request()));
        // the week in which the window closed is still allowed
        assertEquals(LAST_WEEK, service.submit(1L, 10L, LAST_WEEK, request()).weekStart());
    }

    @Test
    void disbandedGroupCannotSubmit() {
        group.disband();
        assertStatus(409, () -> service.submit(1L, 10L, THIS_WEEK, request()));
    }

    @Test
    void sendingTwiceForTheSameWeekKeepsBothReports() {
        report(100L, THIS_WEEK);

        service.submit(1L, 10L, THIS_WEEK, request());
        service.submit(2L, 10L, THIS_WEEK, new WeeklyReportRequest("Second report", "More work", null));

        ArgumentCaptor<WeeklyReport> saved = ArgumentCaptor.forClass(WeeklyReport.class);
        verify(reports, times(2)).save(saved.capture());
        assertEquals("Read three papers", saved.getAllValues().get(0).getDone());
        assertEquals("Second report", saved.getAllValues().get(1).getDone());
        assertEquals(2L, saved.getAllValues().get(1).getSubmittedBy().getId());
        verify(events, times(2)).publishEvent(any(WeeklyReportSubmittedEvent.class));
    }

    @Test
    void anApprovedWeekStillTakesNewReports() {
        WeeklyReport existing = report(100L, THIS_WEEK);
        existing.approve(supervisor, null, LocalDateTime.of(2026, 11, 19, 9, 0));

        WeeklyReportView view = service.submit(1L, 10L, THIS_WEEK, request());

        assertEquals(WeeklyReportStatus.SUBMITTED, view.status());
        assertEquals(WeeklyReportStatus.APPROVED, existing.getStatus());
        assertEquals("Did things", existing.getDone());
    }

    @Test
    void aReturnedReportIsNotReplacedByTheNextOne() {
        WeeklyReport existing = report(100L, THIS_WEEK);
        existing.returnToGroup(supervisor, "Add numbers", LocalDateTime.of(2026, 11, 19, 9, 0));

        WeeklyReportView view = service.submit(1L, 10L, THIS_WEEK, request());

        assertEquals(WeeklyReportStatus.SUBMITTED, view.status());
        assertNull(view.reviewComment());
        assertEquals(WeeklyReportStatus.RETURNED, existing.getStatus());
        assertEquals("Add numbers", existing.getReviewComment());
    }

    // ---- sign-off ----

    @Test
    void supervisorApprovesASubmittedReport() {
        report(100L, THIS_WEEK);

        WeeklyReportView view = service.approve(20L, 100L, "  Good progress ");

        assertEquals(WeeklyReportStatus.APPROVED, view.status());
        assertEquals("Good progress", view.reviewComment());
        assertEquals("User 20", view.reviewedByName());
        ArgumentCaptor<WeeklyReportReviewedEvent> event = ArgumentCaptor.forClass(WeeklyReportReviewedEvent.class);
        verify(events).publishEvent(event.capture());
        assertEquals(WeeklyReportStatus.APPROVED, event.getValue().outcome());
        assertEquals(List.of(1L, 2L), event.getValue().memberIds());
    }

    @Test
    void approvalCommentIsOptional() {
        report(100L, THIS_WEEK);
        assertNull(service.approve(20L, 100L, null).reviewComment());
    }

    @Test
    void returningNeedsAReason() {
        report(100L, THIS_WEEK);
        assertStatus(400, () -> service.returnReport(20L, 100L, "  "));
        assertStatus(400, () -> service.returnReport(20L, 100L, null));
    }

    @Test
    void supervisorReturnsAReportWithAReason() {
        report(100L, THIS_WEEK);

        WeeklyReportView view = service.returnReport(20L, 100L, "Blockers need detail");

        assertEquals(WeeklyReportStatus.RETURNED, view.status());
        assertEquals("Blockers need detail", view.reviewComment());
    }

    @Test
    void onlyASupervisorOfTheGroupCanReview() {
        report(100L, THIS_WEEK);
        assertStatus(403, () -> service.approve(21L, 100L, null));
        assertStatus(403, () -> service.returnReport(21L, 100L, "reason"));
        assertStatus(403, () -> service.approve(1L, 100L, null));
    }

    @Test
    void aReportIsReviewedOnlyOnce() {
        WeeklyReport existing = report(100L, THIS_WEEK);
        existing.approve(supervisor, null, LocalDateTime.of(2026, 11, 19, 9, 0));

        assertStatus(409, () -> service.approve(20L, 100L, null));
        assertStatus(409, () -> service.returnReport(20L, 100L, "too late"));
    }

    @Test
    void unknownReportIsNotFound() {
        assertStatus(404, () -> service.approve(20L, 999L, null));
    }

    @Test
    void pendingListHoldsOnlySubmittedReportsOfMySupervisedGroups() {
        Allocation allocation = new Allocation(group, supervisor, com.thesismate.model.SupervisorRole.SUPERVISOR,
                SEMESTER, null);
        when(allocations.findBySupervisorIdAndStatus(20L, Allocation.Status.ACTIVE)).thenReturn(List.of(allocation));
        WeeklyReport waiting = report(100L, THIS_WEEK);
        when(reports.findByGroup_IdInAndStatusOrderBySubmittedAtAsc(List.of(10L), WeeklyReportStatus.SUBMITTED))
                .thenReturn(List.of(waiting));

        List<WeeklyReportView> pending = service.pendingForSupervisor(20L);

        assertEquals(1, pending.size());
        assertEquals(List.of("User 1", "User 2"), pending.get(0).memberNames());
        assertTrue(service.pendingForSupervisor(21L).isEmpty());
    }

    // ---- missing weeks ----

    @Test
    void pastWeeksWithoutAReportAreMissing() {
        WeeklyReport lastWeeks = report(100L, LAST_WEEK);
        when(reports.findByGroup_IdOrderByWeekStartDescSubmittedAtDesc(10L)).thenReturn(List.of(lastWeeks));

        WeeklyOverviewView view = service.overview(1L, 10L);

        assertEquals(List.of(WEEK_BEFORE), view.missingWeeks());
        assertEquals(WEEK_BEFORE, view.trackingStartsOn());
        assertEquals(THIS_WEEK, view.currentWeek());
        assertFalse(view.currentWeekSubmitted());
        assertTrue(view.canSubmit());
    }

    @Test
    void currentWeekIsNeverMissingYet() {
        WeeklyReport a = report(100L, WEEK_BEFORE);
        WeeklyReport b = report(101L, LAST_WEEK);
        when(reports.findByGroup_IdOrderByWeekStartDescSubmittedAtDesc(10L)).thenReturn(List.of(b, a));

        WeeklyOverviewView view = service.overview(1L, 10L);

        assertTrue(view.missingWeeks().isEmpty());
        assertFalse(view.currentWeekSubmitted());
    }

    @Test
    void returnedReportsAreCountedAndNotMissing() {
        WeeklyReport returned = report(100L, LAST_WEEK);
        returned.returnToGroup(supervisor, "fix", LocalDateTime.of(2026, 11, 18, 9, 0));
        WeeklyReport a = report(101L, WEEK_BEFORE);
        when(reports.findByGroup_IdOrderByWeekStartDescSubmittedAtDesc(10L)).thenReturn(List.of(returned, a));

        WeeklyOverviewView view = service.overview(1L, 10L);

        assertEquals(1, view.returnedCount());
        assertTrue(view.missingWeeks().isEmpty());
    }

    @Test
    void supervisorsAndCoordinatorsCanReadButNotSubmit() {
        WeeklyOverviewView view = service.overview(20L, 10L);
        assertFalse(view.canSubmit());
        assertStatus(403, () -> service.overview(21L, 10L));
        assertStatus(403, () -> service.overview(3L, 10L));
    }

    @Test
    void summaryCountsMissingAndPendingForTheSupervisorPage() {
        WeeklyReport waiting = report(100L, LAST_WEEK);
        when(reports.findByGroup_IdOrderByWeekStartDescSubmittedAtDesc(10L)).thenReturn(List.of(waiting));

        WeeklyReportService.WeeklySummary summary = service.summaryOf(group);

        assertTrue(summary.applicable());
        assertEquals(1, summary.missingWeeks());
        assertEquals(1, summary.pendingReports());
        assertEquals(LAST_WEEK, summary.lastReportWeek());
    }

    // ---- reminders ----

    @Test
    void reminderIsRecordedOnceForTheLatestMissingWeek() {
        when(reports.findGroups(any(), any())).thenReturn(List.of(group));

        int sent = service.sendReminders();

        assertEquals(1, sent);
        ArgumentCaptor<WeeklyReportReminder> saved = ArgumentCaptor.forClass(WeeklyReportReminder.class);
        verify(reminders).save(saved.capture());
        assertEquals(LAST_WEEK, saved.getValue().getWeekStart());
        ArgumentCaptor<WeeklyReportReminderEvent> event = ArgumentCaptor.forClass(WeeklyReportReminderEvent.class);
        verify(events).publishEvent(event.capture());
        assertEquals(2, event.getValue().missingWeeks());
        assertEquals(List.of(1L, 2L), event.getValue().memberIds());
    }

    @Test
    void aWeekThatAlreadyHasAReminderIsSkipped() {
        when(reports.findGroups(any(), any())).thenReturn(List.of(group));
        when(reminders.existsByGroup_IdAndWeekStart(10L, LAST_WEEK)).thenReturn(true);

        assertEquals(0, service.sendReminders());
        verify(reminders, never()).save(any(WeeklyReportReminder.class));
    }

    @Test
    void groupsThatAreUpToDateGetNoReminder() {
        WeeklyReport a = report(100L, WEEK_BEFORE);
        WeeklyReport b = report(101L, LAST_WEEK);
        when(reports.findByGroup_IdOrderByWeekStartDescSubmittedAtDesc(10L)).thenReturn(List.of(b, a));
        when(reports.findGroups(any(), any())).thenReturn(List.of(group));

        assertEquals(0, service.sendReminders());
    }

    @Test
    void groupsWithoutASupervisorGetNoReminder() {
        when(reports.firstAllocationTime(10L)).thenReturn(null);
        when(reports.findGroups(any(), any())).thenReturn(List.of(group));

        assertEquals(0, service.sendReminders());
    }
}
