package com.thesismate.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

import com.thesismate.dto.MilestonePlanView;
import com.thesismate.dto.MilestoneRequest;
import com.thesismate.dto.TaskRequest;
import com.thesismate.dto.TaskView;
import com.thesismate.event.MilestoneCompletedEvent;
import com.thesismate.event.TaskAssignedEvent;
import com.thesismate.model.Allocation;
import com.thesismate.model.AppUser;
import com.thesismate.model.GroupMember;
import com.thesismate.model.Milestone;
import com.thesismate.model.MilestoneTask;
import com.thesismate.model.Phase;
import com.thesismate.model.Role;
import com.thesismate.model.StudentGroup;
import com.thesismate.model.SubmissionWindow;
import com.thesismate.model.TaskSource;
import com.thesismate.model.TaskStatus;
import com.thesismate.model.Track;
import com.thesismate.repository.AllocationRepository;
import com.thesismate.repository.AppUserRepository;
import com.thesismate.repository.GroupMemberRepository;
import com.thesismate.repository.MilestoneRepository;
import com.thesismate.repository.MilestoneTaskRepository;
import com.thesismate.repository.StudentGroupRepository;
import com.thesismate.repository.SubmissionWindowRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MilestoneServiceTest {

    private static final String SEMESTER = "Spring 2027";
    /** Thursday 19 November 2026. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-19T10:00:00Z"), ZoneId.of("UTC"));
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 19);

    @Mock AppUserRepository users;
    @Mock StudentGroupRepository groups;
    @Mock GroupMemberRepository members;
    @Mock AllocationRepository allocations;
    @Mock MilestoneRepository milestones;
    @Mock MilestoneTaskRepository tasks;
    @Mock SubmissionWindowRepository windows;
    @Mock ApplicationEventPublisher events;

    private MilestoneService service;
    private AppUser alice;
    private AppUser bob;
    private AppUser outsider;
    private AppUser supervisor;
    private AppUser coordinator;
    private StudentGroup group;

    @BeforeEach
    void setUp() {
        GroupAccessService access = new GroupAccessService(users, groups, members, allocations);
        service = new MilestoneService(milestones, tasks, groups, members, windows, access, events, CLOCK);

        alice = person(1L, "20000001", Role.STUDENT);
        bob = person(2L, "20000002", Role.STUDENT);
        outsider = person(3L, "20000003", Role.STUDENT);
        supervisor = person(20L, null, Role.SUPERVISOR);
        coordinator = person(30L, null, Role.COORDINATOR);
        group = newGroup(10L, Track.THESIS);

        for (AppUser u : List.of(alice, bob, outsider, supervisor, coordinator)) {
            when(users.findById(u.getId())).thenReturn(Optional.of(u));
        }
        when(groups.findById(10L)).thenReturn(Optional.of(group));
        when(groups.findByIdForUpdate(10L)).thenReturn(Optional.of(group));
        when(members.findByGroupIdAndActiveTrue(10L)).thenReturn(List.of(
                new GroupMember(group, alice), new GroupMember(group, bob)));
        when(milestones.findByGroup_IdOrderBySortOrderAscIdAsc(10L)).thenReturn(List.of());
        when(tasks.findByMilestone_Group_Id(10L)).thenReturn(List.of());
        when(milestones.save(any(Milestone.class))).thenAnswer(i -> i.getArgument(0));
        when(tasks.save(any(MilestoneTask.class))).thenAnswer(i -> i.getArgument(0));
    }

    private AppUser person(long id, String studentId, Role role) {
        AppUser u = new AppUser("u" + id + "@thesismate.test", "User " + id, role, "pw", studentId);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    private StudentGroup newGroup(long id, Track track) {
        StudentGroup g = new StudentGroup(track, SEMESTER, alice == null ? person(1L, "20000001", Role.STUDENT) : alice);
        ReflectionTestUtils.setField(g, "id", id);
        return g;
    }

    private Milestone milestone(long id, LocalDate due) {
        Milestone m = new Milestone(group, "Milestone " + id, null, Phase.PRE_THESIS_1, due, (int) id, false, null, alice);
        ReflectionTestUtils.setField(m, "id", id);
        when(milestones.findById(id)).thenReturn(Optional.of(m));
        return m;
    }

    private MilestoneTask task(long id, Milestone m, TaskStatus status, LocalDate due) {
        MilestoneTask t = new MilestoneTask(m, "Task " + id, null, null, due, TaskSource.MANUAL, null, alice);
        ReflectionTestUtils.setField(t, "id", id);
        if (status != TaskStatus.TODO) {
            t.changeStatus(status, LocalDateTime.of(2026, 11, 18, 9, 0));
        }
        when(tasks.findById(id)).thenReturn(Optional.of(t));
        return t;
    }

    private void assertStatus(int expected, Executable action) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, action);
        assertEquals(expected, ex.getStatusCode().value());
    }

    // ---- who can see the plan ----

    @Test
    void memberSeesAnEditablePlan() {
        MilestonePlanView plan = service.plan(1L, 10L);
        assertTrue(plan.canEdit());
        assertFalse(plan.templateApplied());
        assertEquals(0, plan.summary().completionPercent());
    }

    @Test
    void allocatedSupervisorCanReadButNotEdit() {
        when(allocations.existsByGroupIdAndSupervisorIdAndStatus(10L, 20L, Allocation.Status.ACTIVE)).thenReturn(true);
        assertFalse(service.plan(20L, 10L).canEdit());
    }

    @Test
    void coordinatorCanRead() {
        assertFalse(service.plan(30L, 10L).canEdit());
    }

    @Test
    void supervisorOfAnotherGroupAndOutsidersAreForbidden() {
        assertStatus(403, () -> service.plan(20L, 10L));
        assertStatus(403, () -> service.plan(3L, 10L));
    }

    @Test
    void disbandedGroupIsReadOnlyForMembers() {
        group.disband();
        assertFalse(service.plan(1L, 10L).canEdit());
    }

    // ---- template ----

    @Test
    void templateCreatesTheTrackMilestonesWithDueDatesFromTheSubmissionWindow() {
        SubmissionWindow pt1 = new SubmissionWindow(SEMESTER, Phase.PRE_THESIS_1, Track.THESIS,
                LocalDateTime.of(2026, 12, 1, 9, 0), LocalDateTime.of(2026, 12, 10, 23, 59),
                com.thesismate.model.LatePolicy.NO_LATE, null, coordinator);
        when(windows.findBySemesterAndPhaseAndTrack(SEMESTER, Phase.PRE_THESIS_1, Track.THESIS))
                .thenReturn(Optional.of(pt1));

        service.applyTemplate(1L, 10L);

        ArgumentCaptor<Milestone> saved = ArgumentCaptor.forClass(Milestone.class);
        verify(milestones, times(MilestoneTemplates.forTrack(Track.THESIS).size())).save(saved.capture());
        List<Milestone> list = saved.getAllValues();
        assertTrue(list.stream().allMatch(Milestone::isFromTemplate));
        // 21 days from today to the close date, three milestones: one week apart, the last on the close date.
        assertEquals(LocalDate.of(2026, 11, 26), list.get(0).getDueDate());
        assertEquals(LocalDate.of(2026, 12, 3), list.get(1).getDueDate());
        assertEquals(LocalDate.of(2026, 12, 10), list.get(2).getDueDate());
        // phases without a window get no due date
        assertNull(list.get(3).getDueDate());
        assertEquals(0, list.get(0).getSortOrder());
        assertEquals(8, list.get(8).getSortOrder());
    }

    @Test
    void internshipGroupsGetTheInternshipTemplate() {
        StudentGroup internship = newGroup(11L, Track.INTERNSHIP);
        when(groups.findByIdForUpdate(11L)).thenReturn(Optional.of(internship));
        when(groups.findById(11L)).thenReturn(Optional.of(internship));
        when(members.findByGroupIdAndActiveTrue(11L)).thenReturn(List.of(new GroupMember(internship, alice)));
        when(milestones.findByGroup_IdOrderBySortOrderAscIdAsc(11L)).thenReturn(List.of());
        when(tasks.findByMilestone_Group_Id(11L)).thenReturn(List.of());

        service.applyTemplate(1L, 11L);

        verify(milestones, times(MilestoneTemplates.forTrack(Track.INTERNSHIP).size())).save(any(Milestone.class));
    }

    @Test
    void templateCanBeAddedOnlyOnce() {
        when(milestones.existsByGroup_IdAndFromTemplateTrue(10L)).thenReturn(true);
        assertStatus(409, () -> service.applyTemplate(1L, 10L));
        verify(milestones, never()).save(any(Milestone.class));
    }

    @Test
    void onlyMembersCanAddTheTemplate() {
        assertStatus(403, () -> service.applyTemplate(3L, 10L));
        assertStatus(403, () -> service.applyTemplate(20L, 10L));
    }

    // ---- milestones ----

    @Test
    void memberAddsAMilestoneAtTheEndOfThePlan() {
        Milestone existing = milestone(100L, null);
        when(milestones.findByGroup_IdOrderBySortOrderAscIdAsc(10L)).thenReturn(List.of(existing));

        service.addMilestone(1L, 10L, new MilestoneRequest("  Data collection ", "  ", Phase.PRE_THESIS_2, TODAY.plusDays(30)));

        ArgumentCaptor<Milestone> saved = ArgumentCaptor.forClass(Milestone.class);
        verify(milestones).save(saved.capture());
        assertEquals("Data collection", saved.getValue().getTitle());
        assertNull(saved.getValue().getDescription());
        assertFalse(saved.getValue().isFromTemplate());
        assertEquals(101, saved.getValue().getSortOrder());
    }

    @Test
    void blankMilestoneTitleIsRejected() {
        assertStatus(400, () -> service.addMilestone(1L, 10L, new MilestoneRequest("   ", null, null, null)));
    }

    @Test
    void outsiderCannotAddMilestoneOrEditOne() {
        milestone(100L, null);
        assertStatus(403, () -> service.addMilestone(3L, 10L, new MilestoneRequest("X", null, null, null)));
        assertStatus(403, () -> service.updateMilestone(3L, 100L, new MilestoneRequest("X", null, null, null)));
        assertStatus(403, () -> service.deleteMilestone(20L, 100L));
    }

    @Test
    void deletingAMilestoneRemovesItsTasks() {
        Milestone m = milestone(100L, null);
        MilestoneTask t = task(200L, m, TaskStatus.TODO, null);
        when(tasks.findByMilestone_Id(100L)).thenReturn(List.of(t));

        service.deleteMilestone(1L, 100L);

        verify(tasks).deleteAll(List.of(t));
        verify(milestones).delete(m);
    }

    @Test
    void disbandedGroupCannotBeChanged() {
        group.disband();
        milestone(100L, null);
        assertStatus(409, () -> service.addMilestone(1L, 10L, new MilestoneRequest("X", null, null, null)));
        assertStatus(409, () -> service.applyTemplate(1L, 10L));
        assertStatus(409, () -> service.addTask(1L, 100L, new TaskRequest("T", null, null, null)));
    }

    // ---- tasks ----

    @Test
    void taskCanBeAssignedToAMemberAndTheAssigneeIsTold() {
        milestone(100L, null);

        TaskView view = service.addTask(1L, 100L, new TaskRequest("Write intro", null, 2L, TODAY.plusDays(5)));

        assertEquals(2L, view.assigneeId());
        assertTrue(view.assigneeActive());
        assertEquals(TaskStatus.TODO, view.status());
        assertFalse(view.overdue());
        verify(events).publishEvent(any(TaskAssignedEvent.class));
    }

    @Test
    void assigningATaskToYourselfSendsNoEvent() {
        milestone(100L, null);
        service.addTask(1L, 100L, new TaskRequest("Mine", null, 1L, null));
        verify(events, never()).publishEvent(any(TaskAssignedEvent.class));
    }

    @Test
    void assigneeMustBeAMemberOfTheGroup() {
        milestone(100L, null);
        assertStatus(400, () -> service.addTask(1L, 100L, new TaskRequest("T", null, 3L, null)));
        assertStatus(400, () -> service.addTask(1L, 100L, new TaskRequest("T", null, 20L, null)));
    }

    @Test
    void taskWithoutAssigneeAndDueDateIsFine() {
        milestone(100L, null);
        TaskView view = service.addTask(1L, 100L, new TaskRequest("T", "details", null, null));
        assertNull(view.assigneeId());
        assertFalse(view.assigneeActive());
        assertEquals("details", view.description());
    }

    @Test
    void taskPastItsDueDateIsOverdueUntilItIsDone() {
        Milestone m = milestone(100L, null);
        MilestoneTask t = task(200L, m, TaskStatus.TODO, TODAY.minusDays(2));
        when(tasks.findByMilestone_Id(100L)).thenReturn(List.of(t));

        assertTrue(service.updateTask(1L, 200L, new TaskRequest("Task 200", null, null, TODAY.minusDays(2))).overdue());
        assertFalse(service.changeStatus(1L, 200L, TaskStatus.DONE).overdue());
    }

    @Test
    void doneStatusRecordsCompletionTimeAndReopeningClearsIt() {
        Milestone m = milestone(100L, null);
        MilestoneTask t = task(200L, m, TaskStatus.TODO, null);
        when(tasks.findByMilestone_Id(100L)).thenReturn(List.of(t));

        TaskView done = service.changeStatus(1L, 200L, TaskStatus.DONE);
        assertNotNull(done.completedAt());

        TaskView reopened = service.changeStatus(1L, 200L, TaskStatus.IN_PROGRESS);
        assertNull(reopened.completedAt());
        assertEquals(TaskStatus.IN_PROGRESS, reopened.status());
    }

    @Test
    void finishingTheLastOpenTaskCompletesTheMilestone() {
        Milestone m = milestone(100L, null);
        MilestoneTask first = task(200L, m, TaskStatus.DONE, null);
        MilestoneTask last = task(201L, m, TaskStatus.IN_PROGRESS, null);
        when(tasks.findByMilestone_Id(100L)).thenReturn(List.of(first, last));

        service.changeStatus(1L, 201L, TaskStatus.DONE);

        verify(events).publishEvent(any(MilestoneCompletedEvent.class));
    }

    @Test
    void finishingATaskThatLeavesOthersOpenDoesNotCompleteTheMilestone() {
        Milestone m = milestone(100L, null);
        MilestoneTask first = task(200L, m, TaskStatus.TODO, null);
        MilestoneTask second = task(201L, m, TaskStatus.TODO, null);
        when(tasks.findByMilestone_Id(100L)).thenReturn(List.of(first, second));

        service.changeStatus(1L, 200L, TaskStatus.DONE);

        verify(events, never()).publishEvent(any(MilestoneCompletedEvent.class));
    }

    @Test
    void nonMembersCannotChangeOrDeleteTasks() {
        Milestone m = milestone(100L, null);
        task(200L, m, TaskStatus.TODO, null);
        assertStatus(403, () -> service.changeStatus(3L, 200L, TaskStatus.DONE));
        assertStatus(403, () -> service.changeStatus(20L, 200L, TaskStatus.DONE));
        assertStatus(403, () -> service.deleteTask(3L, 200L));
    }

    @Test
    void unknownMilestoneAndTaskAreNotFound() {
        assertStatus(404, () -> service.addTask(1L, 999L, new TaskRequest("T", null, null, null)));
        assertStatus(404, () -> service.changeStatus(1L, 999L, TaskStatus.DONE));
    }

    // ---- calculated numbers ----

    @Test
    void planSummaryIsCalculatedFromTaskStatusAndDates() {
        Milestone m1 = milestone(100L, TODAY.minusDays(4));
        Milestone m2 = milestone(101L, null);
        MilestoneTask a = task(200L, m1, TaskStatus.DONE, null);
        MilestoneTask b = task(201L, m1, TaskStatus.TODO, TODAY.minusDays(9));
        MilestoneTask c = task(202L, m2, TaskStatus.DONE, null);
        MilestoneTask d = task(203L, m2, TaskStatus.DONE, null);
        when(milestones.findByGroup_IdOrderBySortOrderAscIdAsc(10L)).thenReturn(List.of(m1, m2));
        when(tasks.findByMilestone_Group_Id(10L)).thenReturn(List.of(a, b, c, d));

        MilestonePlanView plan = service.plan(1L, 10L);

        assertEquals(75, plan.summary().completionPercent());
        assertEquals(4, plan.summary().totalTasks());
        assertEquals(3, plan.summary().doneTasks());
        assertEquals(1, plan.summary().overdueTasks());
        assertEquals(1, plan.summary().completedMilestones());
        assertEquals(1, plan.summary().overdueMilestones());
        assertEquals(50, plan.milestones().get(0).completionPercent());
        assertTrue(plan.milestones().get(0).overdue());
        assertEquals("IN_PROGRESS", plan.milestones().get(0).state());
        assertEquals("COMPLETED", plan.milestones().get(1).state());
        assertFalse(plan.milestones().get(1).overdue());
    }

    @Test
    void progressOfCoversEveryGroupEvenWithoutAPlan() {
        assertEquals(0, service.progressOf(10L).completionPercent());
        assertEquals(List.of(10L), List.copyOf(service.progressOf(List.of(10L)).keySet()));
    }

    // ---- tasks created by other features ----

    @Test
    void generatedTaskGoesIntoTheFollowUpMilestone() {
        TaskView view = service.addGeneratedTask(10L, null, "Fix login test", "Failed at demo", TODAY.plusDays(7),
                2L, TaskSource.ACCEPTANCE_TEST, "case-17", 20L);

        ArgumentCaptor<Milestone> bucket = ArgumentCaptor.forClass(Milestone.class);
        verify(milestones).save(bucket.capture());
        assertEquals(Milestone.FOLLOW_UPS_KEY, bucket.getValue().getTemplateKey());
        assertFalse(bucket.getValue().isFromTemplate());
        assertEquals(TaskSource.ACCEPTANCE_TEST, view.source());
        assertEquals("case-17", view.sourceRef());
        assertEquals(2L, view.assigneeId());
    }

    @Test
    void generatedTaskForTheSameOriginIsCreatedOnlyOnce() {
        Milestone bucket = milestone(100L, null);
        MilestoneTask existing = new MilestoneTask(bucket, "Fix login test", null, null, null,
                TaskSource.ACCEPTANCE_TEST, "case-17", supervisor);
        ReflectionTestUtils.setField(existing, "id", 500L);
        when(tasks.findBySourceAndSourceRef(TaskSource.ACCEPTANCE_TEST, "case-17")).thenReturn(Optional.of(existing));

        TaskView view = service.addGeneratedTask(10L, null, "Fix login test", null, null, null,
                TaskSource.ACCEPTANCE_TEST, "case-17", 20L);

        assertEquals(500L, view.id());
        verify(tasks, never()).save(any(MilestoneTask.class));
    }

    @Test
    void generatedTaskNeedsARealSource() {
        assertStatus(400, () -> service.addGeneratedTask(10L, null, "T", null, null, null, TaskSource.MANUAL, null, 20L));
        assertStatus(400, () -> service.addGeneratedTask(10L, null, "T", null, null, null, null, null, 20L));
    }

    @Test
    void generatedTaskMustUseAMilestoneOfTheSameGroup() {
        StudentGroup other = newGroup(11L, Track.PROJECT);
        Milestone foreign = new Milestone(other, "Other", null, null, null, 0, false, null, alice);
        ReflectionTestUtils.setField(foreign, "id", 900L);
        when(milestones.findById(900L)).thenReturn(Optional.of(foreign));

        assertStatus(400, () -> service.addGeneratedTask(10L, 900L, "T", null, null, null,
                TaskSource.MEETING_ACTION, "m-1", 20L));
    }
}
