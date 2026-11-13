package com.thesismate.service;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.thesismate.model.AppUser;
import com.thesismate.model.GroupInvitation;
import com.thesismate.model.GroupMember;
import com.thesismate.model.Role;
import com.thesismate.model.StudentGroup;
import com.thesismate.model.StudentRecord;
import com.thesismate.model.Track;
import com.thesismate.repository.AppUserRepository;
import com.thesismate.repository.GroupInvitationRepository;
import com.thesismate.repository.GroupMemberRepository;
import com.thesismate.repository.StudentGroupRepository;
import com.thesismate.repository.StudentRecordRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GroupServiceTest {

    @Mock AppUserRepository users;
    @Mock StudentRecordRepository records;
    @Mock StudentGroupRepository groups;
    @Mock GroupMemberRepository members;
    @Mock GroupInvitationRepository invitations;

    private GroupService service;
    private AppUser leader;
    private AppUser invitee;
    private StudentGroup group;

    @BeforeEach
    void setUp() {
        service = new GroupService(users, records, groups, members, invitations, new EligibilityService());
        leader = student(1L, "20000001");
        invitee = student(2L, "20000002");
        group = new StudentGroup(Track.THESIS, "Spring 2027", leader);
        ReflectionTestUtils.setField(group, "id", 10L);

        when(users.findById(1L)).thenReturn(Optional.of(leader));
        when(users.findById(2L)).thenReturn(Optional.of(invitee));
        when(users.findByStudentId("20000002")).thenReturn(Optional.of(invitee));
        when(records.findByStudentId("20000001")).thenReturn(Optional.of(record("20000001", 100, "3.50")));
        when(records.findByStudentId("20000002")).thenReturn(Optional.of(record("20000002", 100, "3.20")));
        when(groups.findByIdForUpdate(10L)).thenReturn(Optional.of(group));
        when(groups.save(any(StudentGroup.class))).thenAnswer(i -> i.getArgument(0));
        when(invitations.save(any(GroupInvitation.class))).thenAnswer(i -> i.getArgument(0));
    }

    private AppUser student(long id, String studentId) {
        AppUser u = new AppUser(studentId + "@thesismate.test", "Student " + id, Role.STUDENT, "pw", studentId);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    private StudentRecord record(String studentId, int credits, String cgpa) {
        return new StudentRecord(studentId, credits, new BigDecimal(cgpa), 99L);
    }

    private GroupInvitation invitation() {
        GroupInvitation inv = new GroupInvitation(group, invitee, leader);
        ReflectionTestUtils.setField(inv, "id", 5L);
        when(invitations.findById(5L)).thenReturn(Optional.of(inv));
        return inv;
    }

    private void assertStatus(int expected, Executable action) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, action);
        assertEquals(expected, ex.getStatusCode().value());
    }

    // ---- create group ----

    @Test
    void studentCanCreateGroupAndBecomesMember() {
        service.createGroup(1L, Track.THESIS, "Spring 2027");
        verify(groups).save(any(StudentGroup.class));
        verify(members).save(any(GroupMember.class));
    }

    @Test
    void cannotCreateSecondGroup() {
        when(members.existsByMemberIdAndActiveTrue(1L)).thenReturn(true);
        assertStatus(409, () -> service.createGroup(1L, Track.THESIS, "Spring 2027"));
        verify(groups, never()).save(any(StudentGroup.class));
    }

    @Test
    void ineligibleStudentCannotCreateGroup() {
        when(records.findByStudentId("20000001")).thenReturn(Optional.of(record("20000001", 74, "3.80")));
        assertStatus(400, () -> service.createGroup(1L, Track.THESIS, "Spring 2027"));
    }

    @Test
    void nonStudentCannotCreateGroup() {
        AppUser coordinator = new AppUser("c@thesismate.test", "Coordinator", Role.COORDINATOR, "pw", null);
        ReflectionTestUtils.setField(coordinator, "id", 7L);
        when(users.findById(7L)).thenReturn(Optional.of(coordinator));
        assertStatus(403, () -> service.createGroup(7L, Track.THESIS, "Spring 2027"));
    }

    // ---- invite ----

    @Test
    void leaderCanInviteEligibleStudent() {
        service.inviteByStudentId(1L, 10L, "20000002");
        verify(invitations).save(any(GroupInvitation.class));
    }

    @Test
    void nonLeaderCannotInvite() {
        assertStatus(403, () -> service.inviteByStudentId(2L, 10L, "20000002"));
    }

    @Test
    void unknownStudentIdIsNotFound() {
        assertStatus(404, () -> service.inviteByStudentId(1L, 10L, "29999999"));
    }

    @Test
    void cannotInviteStudentAlreadyInAGroup() {
        when(members.existsByMemberIdAndActiveTrue(2L)).thenReturn(true);
        assertStatus(409, () -> service.inviteByStudentId(1L, 10L, "20000002"));
    }

    @Test
    void cannotInviteIntoFullGroup() {
        when(members.countByGroupIdAndActiveTrue(10L)).thenReturn(5L);
        assertStatus(409, () -> service.inviteByStudentId(1L, 10L, "20000002"));
    }

    @Test
    void cannotInviteIneligibleStudent() {
        when(records.findByStudentId("20000002")).thenReturn(Optional.of(record("20000002", 100, "2.99")));
        assertStatus(400, () -> service.inviteByStudentId(1L, 10L, "20000002"));
    }

    @Test
    void cannotInviteSameStudentTwice() {
        when(invitations.existsByGroupIdAndInviteeIdAndStatus(10L, 2L, GroupInvitation.Status.PENDING))
                .thenReturn(true);
        assertStatus(409, () -> service.inviteByStudentId(1L, 10L, "20000002"));
    }

    // ---- accept and decline ----

    @Test
    void acceptingAddsMemberAndMarksAccepted() {
        GroupInvitation inv = invitation();
        service.respondToInvitation(2L, 5L, true);
        verify(members).save(any(GroupMember.class));
        assertEquals(GroupInvitation.Status.ACCEPTED, inv.getStatus());
    }

    @Test
    void acceptingFailsWhenGroupBecameFull() {
        GroupInvitation inv = invitation();
        when(members.countByGroupIdAndActiveTrue(10L)).thenReturn(5L);
        assertStatus(409, () -> service.respondToInvitation(2L, 5L, true));
        verify(members, never()).save(any(GroupMember.class));
        assertEquals(GroupInvitation.Status.PENDING, inv.getStatus());
    }

    @Test
    void decliningMarksDeclinedAndAddsNobody() {
        GroupInvitation inv = invitation();
        service.respondToInvitation(2L, 5L, false);
        verify(members, never()).save(any(GroupMember.class));
        assertEquals(GroupInvitation.Status.DECLINED, inv.getStatus());
    }

    @Test
    void onlyTheInviteeCanRespond() {
        invitation();
        assertStatus(403, () -> service.respondToInvitation(1L, 5L, true));
    }

    @Test
    void cannotAnswerTheSameInvitationTwice() {
        invitation();
        service.respondToInvitation(2L, 5L, false);
        assertStatus(409, () -> service.respondToInvitation(2L, 5L, true));
    }
}