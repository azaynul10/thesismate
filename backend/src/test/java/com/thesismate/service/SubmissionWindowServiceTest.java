package com.thesismate.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.thesismate.dto.DeliverableRequirementRequest;
import com.thesismate.dto.ExtensionRequest;
import com.thesismate.dto.SubmissionWindowRequest;
import com.thesismate.model.AppUser;
import com.thesismate.model.DeliverableRequirement;
import com.thesismate.model.DeliverableType;
import com.thesismate.model.LatePolicy;
import com.thesismate.model.Phase;
import com.thesismate.model.Role;
import com.thesismate.model.StudentGroup;
import com.thesismate.model.SubmissionExtension;
import com.thesismate.model.SubmissionWindow;
import com.thesismate.model.Track;
import com.thesismate.repository.AppUserRepository;
import com.thesismate.repository.DeliverableRequirementRepository;
import com.thesismate.repository.StudentGroupRepository;
import com.thesismate.repository.SubmissionExtensionRepository;
import com.thesismate.repository.SubmissionWindowRepository;

/** F15 business rules: windows, deliverable checklist, extensions and the checkSubmission rule used by F11. */
@ExtendWith(MockitoExtension.class)
class SubmissionWindowServiceTest {

    private static final String SEMESTER = "Fall 2026";
    private static final LocalDateTime OPENS = LocalDateTime.of(2026, 12, 1, 9, 0);
    private static final LocalDateTime CLOSES = LocalDateTime.of(2026, 12, 15, 23, 59);
    private static final LocalDateTime LATE_UNTIL = LocalDateTime.of(2026, 12, 18, 23, 59);

    @Mock SubmissionWindowRepository windows;
    @Mock DeliverableRequirementRepository requirements;
    @Mock SubmissionExtensionRepository extensions;
    @Mock StudentGroupRepository groups;
    @Mock AppUserRepository users;
    @InjectMocks SubmissionWindowService service;

    // ---------- helpers ----------

    private AppUser coordinator() {
        return new AppUser("coord@t.test", "Coord", Role.COORDINATOR, "x", null);
    }

    private StudentGroup group(Track track, String semester) {
        AppUser leader = new AppUser("s1@t.test", "Student One", Role.STUDENT, "x", "20101001");
        StudentGroup g = new StudentGroup(track, semester, leader);
        ReflectionTestUtils.setField(g, "id", 5L);
        return g;
    }

    private SubmissionWindow window(LatePolicy policy, LocalDateTime lateUntil) {
        SubmissionWindow w = new SubmissionWindow(SEMESTER, Phase.PRE_THESIS_1, Track.THESIS,
                OPENS, CLOSES, policy, lateUntil, coordinator());
        ReflectionTestUtils.setField(w, "id", 1L);
        return w;
    }

    private SubmissionWindowRequest windowRequest(LocalDateTime opens, LocalDateTime closes,
                                                  LatePolicy policy, LocalDateTime lateUntil) {
        return new SubmissionWindowRequest(SEMESTER, Phase.PRE_THESIS_1, Track.THESIS,
                opens, closes, policy, lateUntil);
    }

    private ResponseStatusException assertStatus(HttpStatus expected, org.junit.jupiter.api.function.Executable call) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, call);
        assertEquals(expected, ex.getStatusCode());
        return ex;
    }

    // ---------- windows ----------

    @Test
    void windowThatClosesBeforeItOpensIsRejected() {
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.createWindow("coord@t.test",
                windowRequest(CLOSES, OPENS, LatePolicy.NO_LATE, null)));
        verify(windows, never()).save(any());
    }

    @Test
    void noLatePolicyCannotHaveALateDeadline() {
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.createWindow("coord@t.test",
                windowRequest(OPENS, CLOSES, LatePolicy.NO_LATE, LATE_UNTIL)));
        verify(windows, never()).save(any());
    }

    @Test
    void lateAllowedNeedsALateDeadlineAfterClosing() {
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.createWindow("coord@t.test",
                windowRequest(OPENS, CLOSES, LatePolicy.LATE_ALLOWED, null)));
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.createWindow("coord@t.test",
                windowRequest(OPENS, CLOSES, LatePolicy.LATE_ALLOWED, CLOSES)));
        verify(windows, never()).save(any());
    }

    @Test
    void duplicateWindowForSameSemesterPhaseAndTrackIsRejectedWith409() {
        when(windows.existsBySemesterAndPhaseAndTrack(SEMESTER, Phase.PRE_THESIS_1, Track.THESIS)).thenReturn(true);

        assertStatus(HttpStatus.CONFLICT, () -> service.createWindow("coord@t.test",
                windowRequest(OPENS, CLOSES, LatePolicy.NO_LATE, null)));
        verify(windows, never()).save(any());
    }

    @Test
    void validWindowIsSavedWithTrimmedSemester() {
        when(windows.existsBySemesterAndPhaseAndTrack(SEMESTER, Phase.PRE_THESIS_1, Track.THESIS)).thenReturn(false);
        when(users.findByEmail("coord@t.test")).thenReturn(Optional.of(coordinator()));
        when(windows.save(any(SubmissionWindow.class))).thenAnswer(i -> i.getArgument(0));

        var view = service.createWindow("coord@t.test", new SubmissionWindowRequest("  Fall 2026  ",
                Phase.PRE_THESIS_1, Track.THESIS, OPENS, CLOSES, LatePolicy.LATE_ALLOWED, LATE_UNTIL));

        assertEquals(SEMESTER, view.semester());
        assertEquals(LatePolicy.LATE_ALLOWED, view.latePolicy());
        assertEquals(LATE_UNTIL, view.lateUntil());
        assertEquals("Coord", view.createdByName());
    }

    @Test
    void updatingAnUnknownWindowGives404() {
        when(windows.findById(9L)).thenReturn(Optional.empty());

        assertStatus(HttpStatus.NOT_FOUND, () -> service.updateWindow(9L,
                windowRequest(OPENS, CLOSES, LatePolicy.NO_LATE, null)));
    }

    @Test
    void reschedulingKeepsPhaseAndTrackButChangesTimes() {
        SubmissionWindow w = window(LatePolicy.NO_LATE, null);
        when(windows.findById(1L)).thenReturn(Optional.of(w));
        when(windows.save(any(SubmissionWindow.class))).thenAnswer(i -> i.getArgument(0));
        LocalDateTime newCloses = LocalDateTime.of(2026, 12, 20, 23, 59);

        var view = service.updateWindow(1L, windowRequest(OPENS, newCloses, LatePolicy.NO_LATE, null));

        assertEquals(newCloses, view.closesAt());
        assertEquals(Phase.PRE_THESIS_1, view.phase());
        assertEquals(Track.THESIS, view.track());
    }

    // ---------- deliverable checklist ----------

    @Test
    void duplicateChecklistItemIsRejectedWith409() {
        DeliverableRequirement existing = new DeliverableRequirement(
                Phase.PRE_THESIS_1, Track.THESIS, DeliverableType.REPORT, true, coordinator());
        when(requirements.findByPhaseAndTrackAndDeliverableType(
                Phase.PRE_THESIS_1, Track.THESIS, DeliverableType.REPORT)).thenReturn(Optional.of(existing));

        assertStatus(HttpStatus.CONFLICT, () -> service.addRequirement("coord@t.test",
                new DeliverableRequirementRequest(Phase.PRE_THESIS_1, Track.THESIS, DeliverableType.REPORT, true)));
        verify(requirements, never()).save(any());
    }

    @Test
    void checklistItemIsMandatoryWhenTheFlagIsLeftOut() {
        when(requirements.findByPhaseAndTrackAndDeliverableType(
                Phase.PRE_THESIS_2, Track.THESIS, DeliverableType.POSTER)).thenReturn(Optional.empty());
        when(users.findByEmail("coord@t.test")).thenReturn(Optional.of(coordinator()));
        when(requirements.save(any(DeliverableRequirement.class))).thenAnswer(i -> i.getArgument(0));

        var view = service.addRequirement("coord@t.test",
                new DeliverableRequirementRequest(Phase.PRE_THESIS_2, Track.THESIS, DeliverableType.POSTER, null));

        assertTrue(view.mandatory());
        assertEquals(DeliverableType.POSTER, view.deliverableType());
    }

    @Test
    void deletingAnUnknownChecklistItemGives404() {
        when(requirements.existsById(9L)).thenReturn(false);

        assertStatus(HttpStatus.NOT_FOUND, () -> service.deleteRequirement(9L));
        verify(requirements, never()).deleteById(any());
    }

    @Test
    void deletingAnExistingChecklistItemRemovesIt() {
        when(requirements.existsById(3L)).thenReturn(true);

        service.deleteRequirement(3L);

        verify(requirements).deleteById(3L);
    }

    // ---------- extensions ----------

    private static final LocalDateTime EXT_CLOSES = LocalDateTime.of(2026, 12, 20, 23, 59);

    @Test
    void extensionWithABlankReasonIsRejected() {
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.grantExtension("coord@t.test",
                new ExtensionRequest(5L, 1L, EXT_CLOSES, "   ")));
        verify(extensions, never()).save(any());
    }

    @Test
    void extensionForAnUnknownWindowGives404() {
        when(windows.findById(1L)).thenReturn(Optional.empty());

        assertStatus(HttpStatus.NOT_FOUND, () -> service.grantExtension("coord@t.test",
                new ExtensionRequest(5L, 1L, EXT_CLOSES, "Supervisor meeting delayed")));
    }

    @Test
    void extensionForAGroupOfAnotherTrackIsRejected() {
        when(windows.findById(1L)).thenReturn(Optional.of(window(LatePolicy.NO_LATE, null)));
        when(groups.findById(5L)).thenReturn(Optional.of(group(Track.PROJECT, SEMESTER)));

        assertStatus(HttpStatus.BAD_REQUEST, () -> service.grantExtension("coord@t.test",
                new ExtensionRequest(5L, 1L, EXT_CLOSES, "Supervisor meeting delayed")));
        verify(extensions, never()).save(any());
    }

    @Test
    void extensionForAGroupOfAnotherSemesterIsRejected() {
        when(windows.findById(1L)).thenReturn(Optional.of(window(LatePolicy.NO_LATE, null)));
        when(groups.findById(5L)).thenReturn(Optional.of(group(Track.THESIS, "Spring 2026")));

        assertStatus(HttpStatus.BAD_REQUEST, () -> service.grantExtension("coord@t.test",
                new ExtensionRequest(5L, 1L, EXT_CLOSES, "Supervisor meeting delayed")));
        verify(extensions, never()).save(any());
    }

    @Test
    void extensionMustCloseAfterTheWindowCloses() {
        when(windows.findById(1L)).thenReturn(Optional.of(window(LatePolicy.NO_LATE, null)));
        when(groups.findById(5L)).thenReturn(Optional.of(group(Track.THESIS, SEMESTER)));

        assertStatus(HttpStatus.BAD_REQUEST, () -> service.grantExtension("coord@t.test",
                new ExtensionRequest(5L, 1L, CLOSES, "Supervisor meeting delayed")));
        verify(extensions, never()).save(any());
    }

    @Test
    void validExtensionIsSavedWithTrimmedReason() {
        when(windows.findById(1L)).thenReturn(Optional.of(window(LatePolicy.NO_LATE, null)));
        when(groups.findById(5L)).thenReturn(Optional.of(group(Track.THESIS, SEMESTER)));
        when(users.findByEmail("coord@t.test")).thenReturn(Optional.of(coordinator()));
        when(extensions.findByGroup_IdAndWindow_Id(5L, 1L)).thenReturn(Optional.empty());
        when(extensions.save(any(SubmissionExtension.class))).thenAnswer(i -> i.getArgument(0));

        var view = service.grantExtension("coord@t.test",
                new ExtensionRequest(5L, 1L, EXT_CLOSES, "  Supervisor meeting delayed  "));

        assertEquals(EXT_CLOSES, view.newClosesAt());
        assertEquals("Supervisor meeting delayed", view.reason());
        assertEquals("Coord", view.grantedByName());
    }

    @Test
    void grantingAgainUpdatesTheExistingExtensionInsteadOfAddingAnother() {
        SubmissionWindow w = window(LatePolicy.NO_LATE, null);
        StudentGroup g = group(Track.THESIS, SEMESTER);
        SubmissionExtension existing = new SubmissionExtension(g, w, EXT_CLOSES, "First reason", coordinator());
        LocalDateTime later = LocalDateTime.of(2026, 12, 22, 23, 59);
        when(windows.findById(1L)).thenReturn(Optional.of(w));
        when(groups.findById(5L)).thenReturn(Optional.of(g));
        when(users.findByEmail("coord@t.test")).thenReturn(Optional.of(coordinator()));
        when(extensions.findByGroup_IdAndWindow_Id(5L, 1L)).thenReturn(Optional.of(existing));
        when(extensions.save(any(SubmissionExtension.class))).thenAnswer(i -> i.getArgument(0));

        service.grantExtension("coord@t.test", new ExtensionRequest(5L, 1L, later, "Second reason"));

        assertEquals(later, existing.getNewClosesAt());
        assertEquals("Second reason", existing.getReason());
        verify(extensions).save(existing);
    }

    // ---------- checkSubmission (the rule F11 calls) ----------

    @Test
    void checkForAnUnknownGroupGives404() {
        when(groups.findById(5L)).thenReturn(Optional.empty());

        assertStatus(HttpStatus.NOT_FOUND,
                () -> service.checkSubmission(5L, Phase.PRE_THESIS_1, OPENS.plusDays(1)));
    }

    @Test
    void noWindowMeansNoSubmission() {
        when(groups.findById(5L)).thenReturn(Optional.of(group(Track.THESIS, SEMESTER)));
        when(windows.findBySemesterAndPhaseAndTrack(SEMESTER, Phase.PRE_THESIS_1, Track.THESIS))
                .thenReturn(Optional.empty());

        var check = service.checkSubmission(5L, Phase.PRE_THESIS_1, OPENS.plusDays(1));

        assertFalse(check.allowed());
        assertFalse(check.late());
    }

    @Test
    void beforeTheWindowOpensSubmissionIsNotAllowed() {
        when(groups.findById(5L)).thenReturn(Optional.of(group(Track.THESIS, SEMESTER)));
        when(windows.findBySemesterAndPhaseAndTrack(SEMESTER, Phase.PRE_THESIS_1, Track.THESIS))
                .thenReturn(Optional.of(window(LatePolicy.NO_LATE, null)));

        var check = service.checkSubmission(5L, Phase.PRE_THESIS_1, OPENS.minusMinutes(1));

        assertFalse(check.allowed());
    }

    @Test
    void whileTheWindowIsOpenSubmissionIsAllowedAndNotLate() {
        when(groups.findById(5L)).thenReturn(Optional.of(group(Track.THESIS, SEMESTER)));
        when(windows.findBySemesterAndPhaseAndTrack(SEMESTER, Phase.PRE_THESIS_1, Track.THESIS))
                .thenReturn(Optional.of(window(LatePolicy.NO_LATE, null)));
        when(extensions.findByGroup_IdAndWindow_Id(5L, 1L)).thenReturn(Optional.empty());

        var check = service.checkSubmission(5L, Phase.PRE_THESIS_1, OPENS.plusDays(3));

        assertTrue(check.allowed());
        assertFalse(check.late());
    }

    @Test
    void afterClosingWithNoLatePolicySubmissionIsNotAllowed() {
        when(groups.findById(5L)).thenReturn(Optional.of(group(Track.THESIS, SEMESTER)));
        when(windows.findBySemesterAndPhaseAndTrack(SEMESTER, Phase.PRE_THESIS_1, Track.THESIS))
                .thenReturn(Optional.of(window(LatePolicy.NO_LATE, null)));
        when(extensions.findByGroup_IdAndWindow_Id(5L, 1L)).thenReturn(Optional.empty());

        var check = service.checkSubmission(5L, Phase.PRE_THESIS_1, CLOSES.plusMinutes(1));

        assertFalse(check.allowed());
    }

    @Test
    void afterClosingWithLateAllowedSubmissionIsAllowedButLate() {
        when(groups.findById(5L)).thenReturn(Optional.of(group(Track.THESIS, SEMESTER)));
        when(windows.findBySemesterAndPhaseAndTrack(SEMESTER, Phase.PRE_THESIS_1, Track.THESIS))
                .thenReturn(Optional.of(window(LatePolicy.LATE_ALLOWED, LATE_UNTIL)));
        when(extensions.findByGroup_IdAndWindow_Id(5L, 1L)).thenReturn(Optional.empty());

        var check = service.checkSubmission(5L, Phase.PRE_THESIS_1, CLOSES.plusDays(1));

        assertTrue(check.allowed());
        assertTrue(check.late());
    }

    @Test
    void afterTheLateDeadlineSubmissionIsNotAllowed() {
        when(groups.findById(5L)).thenReturn(Optional.of(group(Track.THESIS, SEMESTER)));
        when(windows.findBySemesterAndPhaseAndTrack(SEMESTER, Phase.PRE_THESIS_1, Track.THESIS))
                .thenReturn(Optional.of(window(LatePolicy.LATE_ALLOWED, LATE_UNTIL)));
        when(extensions.findByGroup_IdAndWindow_Id(5L, 1L)).thenReturn(Optional.empty());

        var check = service.checkSubmission(5L, Phase.PRE_THESIS_1, LATE_UNTIL.plusMinutes(1));

        assertFalse(check.allowed());
    }

    @Test
    void anExtensionKeepsTheWindowOpenForThatGroupAndIsNotLate() {
        SubmissionWindow w = window(LatePolicy.NO_LATE, null);
        StudentGroup g = group(Track.THESIS, SEMESTER);
        SubmissionExtension ext = new SubmissionExtension(g, w, EXT_CLOSES, "Supervisor meeting delayed", coordinator());
        when(groups.findById(5L)).thenReturn(Optional.of(g));
        when(windows.findBySemesterAndPhaseAndTrack(SEMESTER, Phase.PRE_THESIS_1, Track.THESIS))
                .thenReturn(Optional.of(w));
        when(extensions.findByGroup_IdAndWindow_Id(5L, 1L)).thenReturn(Optional.of(ext));

        var inside = service.checkSubmission(5L, Phase.PRE_THESIS_1, CLOSES.plusDays(2));
        var after = service.checkSubmission(5L, Phase.PRE_THESIS_1, EXT_CLOSES.plusMinutes(1));

        assertTrue(inside.allowed());
        assertFalse(inside.late());
        assertFalse(after.allowed());
        assertFalse(after.late());
    }
}
