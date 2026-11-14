package com.thesismate.service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.thesismate.model.Allocation;
import com.thesismate.model.AppUser;
import com.thesismate.model.GroupMember;
import com.thesismate.model.Role;
import com.thesismate.model.StudentGroup;
import com.thesismate.model.SupervisionRequest;
import com.thesismate.model.SupervisorRole;
import com.thesismate.model.Track;
import com.thesismate.repository.AllocationRepository;
import com.thesismate.repository.AppUserRepository;
import com.thesismate.repository.GroupMemberRepository;
import com.thesismate.repository.StudentGroupRepository;
import com.thesismate.repository.SupervisionRequestRepository;

import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SupervisionRequestServiceTest {

    private static final String SEMESTER = "Spring 2027";

    @Mock AppUserRepository users;
    @Mock StudentGroupRepository groups;
    @Mock GroupMemberRepository members;
    @Mock SupervisionRequestRepository requests;
    @Mock AllocationRepository allocations;
    @Mock SupervisorCapacityLookup lookup;
    @Mock EntityManager em;

    private SupervisionRequestService service;
    private AppUser leader;
    private AppUser otherStudent;
    private AppUser supervisor;
    private StudentGroup group;

    @BeforeEach
    void setUp() {
        service = new SupervisionRequestService(users, groups, members, requests, allocations, lookup, em);
        leader = person(1L, "20000001", Role.STUDENT);
        otherStudent = person(2L, "20000002", Role.STUDENT);
        supervisor = person(20L, null, Role.SUPERVISOR);
        group = new StudentGroup(Track.THESIS, SEMESTER, leader);
        ReflectionTestUtils.setField(group, "id", 10L);

        when(users.findById(1L)).thenReturn(Optional.of(leader));
        when(users.findById(2L)).thenReturn(Optional.of(otherStudent));
        when(users.findById(20L)).thenReturn(Optional.of(supervisor));
        when(groups.findByIdForUpdate(10L)).thenReturn(Optional.of(group));
        when(lookup.tracksFor(20L)).thenReturn(EnumSet.allOf(Track.class));
        when(lookup.capacityFor(20L, SEMESTER)).thenReturn(3);
        when(requests.save(any(SupervisionRequest.class))).thenAnswer(i -> i.getArgument(0));
    }

    private AppUser person(long id, String studentId, Role role) {
        AppUser u = new AppUser("u" + id + "@thesismate.test", "User " + id, role, "pw", studentId);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    private SupervisionRequest pending(long id, LocalDateTime expiresAt) {
        SupervisionRequest r = new SupervisionRequest(group, supervisor, SupervisorRole.SUPERVISOR,
                "Our thesis idea", leader, expiresAt);
        ReflectionTestUtils.setField(r, "id", id);
        when(requests.findById(id)).thenReturn(Optional.of(r));
        return r;
    }

    private SupervisionRequest groupRequest(long id, long supervisorId, SupervisorRole role) {
        SupervisionRequest r = new SupervisionRequest(group, person(supervisorId, null, Role.SUPERVISOR), role,
                "Another pitch", leader, LocalDateTime.now().plusDays(3));
        ReflectionTestUtils.setField(r, "id", id);
        return r;
    }

    private void assertStatus(int expected, Executable action) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, action);
        assertEquals(expected, ex.getStatusCode().value());
    }

    // ---- sending ----

    @Test
    void leaderCanSendRequest() {
        service.sendRequest(1L, 10L, 20L, SupervisorRole.SUPERVISOR, "Our thesis idea");
        verify(requests).save(any(SupervisionRequest.class));
    }

    @Test
    void onlyTheLeaderCanSend() {
        assertStatus(403, () -> service.sendRequest(2L, 10L, 20L, SupervisorRole.SUPERVISOR, "Pitch"));
    }

    @Test
    void cannotSendFourthPendingRequest() {
        when(requests.countByGroupIdAndStatus(10L, SupervisionRequest.Status.PENDING)).thenReturn(3L);
        assertStatus(409, () -> service.sendRequest(1L, 10L, 20L, SupervisorRole.SUPERVISOR, "Pitch"));
    }

    @Test
    void cannotSendTwoPendingRequestsToTheSameSupervisor() {
        when(requests.existsByGroupIdAndSupervisorIdAndStatus(10L, 20L, SupervisionRequest.Status.PENDING))
                .thenReturn(true);
        assertStatus(409, () -> service.sendRequest(1L, 10L, 20L, SupervisorRole.SUPERVISOR, "Pitch"));
    }

    @Test
    void targetMustBeASupervisor() {
        assertStatus(400, () -> service.sendRequest(1L, 10L, 2L, SupervisorRole.SUPERVISOR, "Pitch"));
    }

    @Test
    void unknownSupervisorIsNotFound() {
        assertStatus(404, () -> service.sendRequest(1L, 10L, 99L, SupervisorRole.SUPERVISOR, "Pitch"));
    }

    @Test
    void supervisorMustTakeTheGroupsTrack() {
        when(lookup.tracksFor(20L)).thenReturn(EnumSet.of(Track.PROJECT));
        assertStatus(400, () -> service.sendRequest(1L, 10L, 20L, SupervisorRole.SUPERVISOR, "Pitch"));
    }

    @Test
    void cannotRequestSomeoneAlreadyAssignedToTheGroup() {
        when(allocations.existsByGroupIdAndSupervisorIdAndStatus(10L, 20L, Allocation.Status.ACTIVE))
                .thenReturn(true);
        assertStatus(409, () -> service.sendRequest(1L, 10L, 20L, SupervisorRole.SUPERVISOR, "Pitch"));
    }

    @Test
    void cannotRequestAThirdCoSupervisor() {
        when(allocations.countByGroupIdAndSupervisorRoleAndStatus(
                10L, SupervisorRole.CO_SUPERVISOR, Allocation.Status.ACTIVE)).thenReturn(2L);
        assertStatus(409, () -> service.sendRequest(1L, 10L, 20L, SupervisorRole.CO_SUPERVISOR, "Pitch"));
    }

    @Test
    void cannotRequestAFullSupervisor() {
        when(allocations.countBySupervisorIdAndSemesterAndStatus(20L, SEMESTER, Allocation.Status.ACTIVE))
                .thenReturn(3L);
        assertStatus(409, () -> service.sendRequest(1L, 10L, 20L, SupervisorRole.SUPERVISOR, "Pitch"));
    }

    @Test
    void pitchCannotBeBlank() {
        assertStatus(400, () -> service.sendRequest(1L, 10L, 20L, SupervisorRole.SUPERVISOR, "  "));
    }

    // ---- withdrawing ----

    @Test
    void leaderCanWithdrawAPendingRequest() {
        SupervisionRequest r = pending(5L, LocalDateTime.now().plusDays(3));
        service.withdraw(1L, 5L);
        assertEquals(SupervisionRequest.Status.WITHDRAWN, r.getStatus());
    }

    @Test
    void onlyTheLeaderCanWithdraw() {
        pending(5L, LocalDateTime.now().plusDays(3));
        assertStatus(403, () -> service.withdraw(2L, 5L));
    }

    @Test
    void cannotWithdrawTwice() {
        pending(5L, LocalDateTime.now().plusDays(3));
        service.withdraw(1L, 5L);
        assertStatus(409, () -> service.withdraw(1L, 5L));
    }

    // ---- rejecting ----

    @Test
    void supervisorCanRejectWithAReason() {
        SupervisionRequest r = pending(5L, LocalDateTime.now().plusDays(3));
        service.reject(20L, 5L, "Fully booked this semester");
        assertEquals(SupervisionRequest.Status.REJECTED, r.getStatus());
        assertEquals("Fully booked this semester", r.getRejectionReason());
    }

    @Test
    void rejectingNeedsAReason() {
        SupervisionRequest r = pending(5L, LocalDateTime.now().plusDays(3));
        assertStatus(400, () -> service.reject(20L, 5L, " "));
        assertEquals(SupervisionRequest.Status.PENDING, r.getStatus());
    }

    @Test
    void onlyTheAddressedSupervisorCanReject() {
        pending(5L, LocalDateTime.now().plusDays(3));
        assertStatus(403, () -> service.reject(21L, 5L, "No"));
    }

    @Test
    void cannotRejectAnExpiredRequest() {
        SupervisionRequest r = pending(5L, LocalDateTime.now().minusDays(1));
        assertStatus(409, () -> service.reject(20L, 5L, "No"));
        assertEquals(SupervisionRequest.Status.PENDING, r.getStatus());
    }

    // ---- lists ----

    @Test
    void supervisorSeesOnlyRequestsThatHaveNotExpired() {
        SupervisionRequest live = pending(5L, LocalDateTime.now().plusDays(3));
        SupervisionRequest old = pending(6L, LocalDateTime.now().minusDays(1));
        when(requests.findBySupervisorIdAndStatusOrderByCreatedAtAsc(20L, SupervisionRequest.Status.PENDING))
                .thenReturn(List.of(live, old));
        assertEquals(1, service.getPendingForSupervisor(20L).size());
    }

    @Test
    void nonMembersCannotListAGroupsRequests() {
        when(members.findByGroupIdAndActiveTrue(10L)).thenReturn(List.of(new GroupMember(group, leader)));
        assertStatus(403, () -> service.getGroupRequests(2L, 10L));
    }

    @Test
    void membersCanListTheirGroupsRequests() {
        when(members.findByGroupIdAndActiveTrue(10L)).thenReturn(List.of(new GroupMember(group, leader)));
        assertTrue(service.getGroupRequests(1L, 10L).isEmpty());
    }

    // ---- accepting ----

    @Test
    void supervisorCanAccept() {
        SupervisionRequest r = pending(5L, LocalDateTime.now().plusDays(3));
        service.accept(20L, 5L);
        assertEquals(SupervisionRequest.Status.ACCEPTED, r.getStatus());
        verify(allocations).save(any(Allocation.class));
    }

    @Test
    void onlyTheAddressedSupervisorCanAccept() {
        pending(5L, LocalDateTime.now().plusDays(3));
        assertStatus(403, () -> service.accept(21L, 5L));
        verify(allocations, never()).save(any(Allocation.class));
    }

    @Test
    void cannotAcceptAnExpiredRequest() {
        SupervisionRequest r = pending(5L, LocalDateTime.now().minusDays(1));
        assertStatus(409, () -> service.accept(20L, 5L));
        assertEquals(SupervisionRequest.Status.PENDING, r.getStatus());
        verify(allocations, never()).save(any(Allocation.class));
    }

    @Test
    void cannotAcceptTwice() {
        pending(5L, LocalDateTime.now().plusDays(3));
        service.accept(20L, 5L);
        assertStatus(409, () -> service.accept(20L, 5L));
        verify(allocations, times(1)).save(any(Allocation.class));
    }

    @Test
    void cannotAcceptWhenTheSupervisorIsFull() {
        SupervisionRequest r = pending(5L, LocalDateTime.now().plusDays(3));
        when(allocations.countBySupervisorIdAndSemesterAndStatus(20L, SEMESTER, Allocation.Status.ACTIVE))
                .thenReturn(3L);
        assertStatus(409, () -> service.accept(20L, 5L));
        assertEquals(SupervisionRequest.Status.PENDING, r.getStatus());
        verify(allocations, never()).save(any(Allocation.class));
    }

    @Test
    void cannotAcceptWhenTheGroupAlreadyHasTwoInThatRole() {
        pending(5L, LocalDateTime.now().plusDays(3));
        when(allocations.countByGroupIdAndSupervisorRoleAndStatus(
                10L, SupervisorRole.SUPERVISOR, Allocation.Status.ACTIVE)).thenReturn(2L);
        assertStatus(409, () -> service.accept(20L, 5L));
        verify(allocations, never()).save(any(Allocation.class));
    }

    @Test
    void cannotAcceptWhenAlreadyAssignedToTheGroup() {
        pending(5L, LocalDateTime.now().plusDays(3));
        when(allocations.existsByGroupIdAndSupervisorIdAndStatus(10L, 20L, Allocation.Status.ACTIVE))
                .thenReturn(true);
        assertStatus(409, () -> service.accept(20L, 5L));
    }

    @Test
    void cannotAcceptWhenTheSupervisorNoLongerTakesTheTrack() {
        pending(5L, LocalDateTime.now().plusDays(3));
        when(lookup.tracksFor(20L)).thenReturn(EnumSet.of(Track.PROJECT));
        assertStatus(400, () -> service.accept(20L, 5L));
    }

    @Test
    void fillingARoleWithdrawsTheOtherPendingRequestsForThatRole() {
        SupervisionRequest accepted = pending(5L, LocalDateTime.now().plusDays(3));
        SupervisionRequest sameRole = groupRequest(6L, 21L, SupervisorRole.SUPERVISOR);
        SupervisionRequest otherRole = groupRequest(7L, 22L, SupervisorRole.CO_SUPERVISOR);
        when(allocations.countByGroupIdAndSupervisorRoleAndStatus(
                10L, SupervisorRole.SUPERVISOR, Allocation.Status.ACTIVE)).thenReturn(1L);
        when(requests.findByGroupIdAndStatus(10L, SupervisionRequest.Status.PENDING))
                .thenReturn(List.of(accepted, sameRole, otherRole));

        service.accept(20L, 5L);

        assertEquals(SupervisionRequest.Status.WITHDRAWN, sameRole.getStatus());
        assertEquals(SupervisionRequest.Status.PENDING, otherRole.getStatus());
    }

    @Test
    void otherRequestsStayWhileTheRoleHasRoomLeft() {
        SupervisionRequest accepted = pending(5L, LocalDateTime.now().plusDays(3));
        SupervisionRequest sameRole = groupRequest(6L, 21L, SupervisorRole.SUPERVISOR);
        when(requests.findByGroupIdAndStatus(10L, SupervisionRequest.Status.PENDING))
                .thenReturn(List.of(accepted, sameRole));

        service.accept(20L, 5L);

        assertEquals(SupervisionRequest.Status.PENDING, sameRole.getStatus());
    }
}