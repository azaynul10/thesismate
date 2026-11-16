package com.thesismate.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.thesismate.dto.ReportView;
import com.thesismate.model.AppUser;
import com.thesismate.model.GroupMember;
import com.thesismate.model.Phase;
import com.thesismate.model.PlagiarismReport;
import com.thesismate.model.Report;
import com.thesismate.model.ReportStatus;
import com.thesismate.model.ReportVersion;
import com.thesismate.model.Role;
import com.thesismate.model.StudentGroup;
import com.thesismate.model.Track;
import com.thesismate.repository.AppUserRepository;
import com.thesismate.repository.GroupMemberRepository;
import com.thesismate.repository.PlagiarismReportRepository;
import com.thesismate.repository.ReportRepository;
import com.thesismate.repository.ReportVersionRepository;
import com.thesismate.repository.StudentGroupRepository;
import com.thesismate.service.FileStorageService.StoredFile;
import com.thesismate.service.ReportService.DownloadFile;
import com.thesismate.service.ReportService.FileKind;
import com.thesismate.service.SubmissionWindowService.SubmissionCheck;

/** F11 business rules: window check, Turnitin gate, locked reports, versions, access rules. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReportServiceTest {

    private static final Long STUDENT_ID = 5L;
    private static final Long GROUP_ID = 7L;
    private static final Phase PHASE = Phase.PRE_THESIS_1;

    @Mock ReportRepository reports;
    @Mock ReportVersionRepository versions;
    @Mock PlagiarismReportRepository plagiarism;
    @Mock StudentGroupRepository groups;
    @Mock GroupMemberRepository members;
    @Mock AppUserRepository users;
    @Mock SubmissionWindowService windows;
    @Mock FileStorageService storage;
    @InjectMocks ReportService service;

    private AppUser student;
    private StudentGroup group;

    @BeforeEach
    void setUp() {
        student = user(STUDENT_ID, "Student Five", Role.STUDENT);
        group = new StudentGroup(Track.THESIS, "Fall 2026", student);
        ReflectionTestUtils.setField(group, "id", GROUP_ID);

        when(users.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        when(groups.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(members.findByGroupIdAndActiveTrue(GROUP_ID)).thenReturn(List.of(new GroupMember(group, student)));
        when(windows.checkSubmission(eq(GROUP_ID), eq(PHASE), any(LocalDateTime.class)))
                .thenReturn(new SubmissionCheck(true, false, "Window is open"));
        when(reports.findByGroup_IdAndPhase(GROUP_ID, PHASE)).thenReturn(Optional.empty());
        when(reports.save(any(Report.class))).thenAnswer(inv -> inv.getArgument(0));
        when(versions.findFirstByReport_IdOrderByVersionNoDesc(any())).thenReturn(Optional.empty());
        when(versions.findByReport_IdOrderByVersionNoDesc(any())).thenReturn(List.of());
        when(versions.save(any(ReportVersion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(plagiarism.save(any(PlagiarismReport.class))).thenAnswer(inv -> inv.getArgument(0));
        when(plagiarism.findByVersion_Id(any())).thenReturn(Optional.empty());
        when(storage.store(any(), anyString())).thenReturn(new StoredFile("stored-key.pdf", 21L, "abc123"));
    }

    // ---------- helpers ----------

    private AppUser user(Long id, String name, Role role) {
        AppUser u = new AppUser("u" + id + "@t.test", name, role, "x", role == Role.STUDENT ? "2010" + id : null);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    private MockMultipartFile pdf(String name) {
        return new MockMultipartFile(name, name + ".pdf", "application/pdf", "%PDF-1.4 x".getBytes());
    }

    private ReportView submit(BigDecimal percent) {
        return service.submit(STUDENT_ID, GROUP_ID, PHASE, percent, pdf("report"), pdf("similarity"), pdf("ai"));
    }

    private ResponseStatusException submitFails(BigDecimal percent) {
        return assertThrows(ResponseStatusException.class, () -> submit(percent));
    }

    private Report existingReport(ReportStatus status) {
        Report r = new Report(group, PHASE);
        ReflectionTestUtils.setField(r, "id", 100L);
        r.changeStatus(status);
        when(reports.findByGroup_IdAndPhase(GROUP_ID, PHASE)).thenReturn(Optional.of(r));
        return r;
    }

    private ReportVersion version(Report r, int no) {
        ReportVersion v = new ReportVersion(r, no, "report-v" + no + ".pdf", "report-key-" + no + ".pdf",
                21L, "hash" + no, false, student);
        ReflectionTestUtils.setField(v, "id", 50L);
        return v;
    }

    private void windowResult(boolean allowed, boolean late, String reason) {
        when(windows.checkSubmission(eq(GROUP_ID), eq(PHASE), any(LocalDateTime.class)))
                .thenReturn(new SubmissionCheck(allowed, late, reason));
    }

    // ---------- submit: who and when ----------

    @Test
    void submit_userNotInGroup_is403AndNothingStored() {
        when(members.findByGroupIdAndActiveTrue(GROUP_ID))
                .thenReturn(List.of(new GroupMember(group, user(99L, "Someone Else", Role.STUDENT))));

        ResponseStatusException e = submitFails(new BigDecimal("10"));

        assertEquals(403, e.getStatusCode().value());
        verify(storage, never()).store(any(), anyString());
    }

    @Test
    void submit_unknownGroup_is404() {
        when(groups.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.submit(STUDENT_ID, 999L, PHASE, new BigDecimal("10"),
                        pdf("report"), pdf("similarity"), pdf("ai")));

        assertEquals(404, e.getStatusCode().value());
    }

    @Test
    void submit_disbandedGroup_is400() {
        group.disband();

        ResponseStatusException e = submitFails(new BigDecimal("10"));

        assertEquals(400, e.getStatusCode().value());
        verify(storage, never()).store(any(), anyString());
    }

    @Test
    void submit_missingPhase_is400() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.submit(STUDENT_ID, GROUP_ID, null, new BigDecimal("10"),
                        pdf("report"), pdf("similarity"), pdf("ai")));

        assertEquals(400, e.getStatusCode().value());
    }

    @Test
    void submit_windowClosed_is403WithTheWindowReason() {
        windowResult(false, false, "The submission window has closed");

        ResponseStatusException e = submitFails(new BigDecimal("10"));

        assertEquals(403, e.getStatusCode().value());
        assertEquals("The submission window has closed", e.getReason());
        verify(storage, never()).store(any(), anyString());
        verify(versions, never()).save(any(ReportVersion.class));
    }

    @Test
    void submit_lateButAllowed_marksTheVersionLate() {
        windowResult(true, true, "Window is closed; late submission is allowed");

        submit(new BigDecimal("10"));

        ArgumentCaptor<ReportVersion> saved = ArgumentCaptor.forClass(ReportVersion.class);
        verify(versions).save(saved.capture());
        assertTrue(saved.getValue().isLate());
    }

    // ---------- submit: Turnitin gate ----------

    @Test
    void submit_similarityAboveLimit_is400AndNothingStored() {
        ResponseStatusException e = submitFails(new BigDecimal("15.01"));

        assertEquals(400, e.getStatusCode().value());
        assertTrue(e.getReason().contains("15"));
        verify(storage, never()).store(any(), anyString());
        verify(versions, never()).save(any(ReportVersion.class));
    }

    @Test
    void submit_similarityExactlyAtLimit_isAccepted() {
        ReportView view = submit(new BigDecimal("15"));

        assertEquals(GROUP_ID, view.groupId());
        verify(versions).save(any(ReportVersion.class));
    }

    @Test
    void submit_similarityMissing_is400() {
        ResponseStatusException e = submitFails(null);

        assertEquals(400, e.getStatusCode().value());
        verify(storage, never()).store(any(), anyString());
    }

    @Test
    void submit_similarityNegative_is400() {
        assertEquals(400, submitFails(new BigDecimal("-1")).getStatusCode().value());
    }

    @Test
    void submit_similarityOver100_is400() {
        assertEquals(400, submitFails(new BigDecimal("101")).getStatusCode().value());
    }

    @Test
    void submit_missingTurnitinAiReport_is400() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.submit(STUDENT_ID, GROUP_ID, PHASE, new BigDecimal("10"),
                        pdf("report"), pdf("similarity"), null));

        assertEquals(400, e.getStatusCode().value());
        assertEquals("The Turnitin AI report is required", e.getReason());
        verify(storage, never()).store(any(), anyString());
    }

    @Test
    void submit_emptyTurnitinSimilarityReport_is400() {
        MockMultipartFile empty = new MockMultipartFile("similarity", "similarity.pdf", "application/pdf", new byte[0]);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.submit(STUDENT_ID, GROUP_ID, PHASE, new BigDecimal("10"),
                        pdf("report"), empty, pdf("ai")));

        assertEquals(400, e.getStatusCode().value());
        assertEquals("The Turnitin similarity report is required", e.getReason());
    }

    @Test
    void submit_missingReportFile_is400() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.submit(STUDENT_ID, GROUP_ID, PHASE, new BigDecimal("10"),
                        null, pdf("similarity"), pdf("ai")));

        assertEquals(400, e.getStatusCode().value());
    }

    @Test
    void submit_fileThatIsNotAPdf_isRejectedAndNothingStored() {
        MockMultipartFile bad = new MockMultipartFile("report", "report.pdf", "application/pdf", "hello".getBytes());
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "The report PDF is not a valid PDF file"))
                .when(storage).validatePdf(same(bad), anyString());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.submit(STUDENT_ID, GROUP_ID, PHASE, new BigDecimal("10"),
                        bad, pdf("similarity"), pdf("ai")));

        assertEquals(400, e.getStatusCode().value());
        verify(storage, never()).store(any(), anyString());
    }

    // ---------- submit: locking and versions ----------

    @Test
    void submit_supervisorApprovedReport_cannotBeReplaced() {
        existingReport(ReportStatus.SUPERVISOR_APPROVED);

        ResponseStatusException e = submitFails(new BigDecimal("10"));

        assertEquals(409, e.getStatusCode().value());
        verify(storage, never()).store(any(), anyString());
        verify(versions, never()).save(any(ReportVersion.class));
    }

    @Test
    void submit_committeeApprovedReport_cannotBeReplaced() {
        existingReport(ReportStatus.COMMITTEE_APPROVED);

        assertEquals(409, submitFails(new BigDecimal("10")).getStatusCode().value());
    }

    @Test
    void submit_firstUpload_createsReportAndVersion1() {
        ReportView view = submit(new BigDecimal("12.5"));

        ArgumentCaptor<ReportVersion> version = ArgumentCaptor.forClass(ReportVersion.class);
        verify(versions).save(version.capture());
        assertEquals(1, version.getValue().getVersionNo());
        assertFalse(version.getValue().isLate());
        assertFalse(version.getValue().isLocked());
        assertEquals("report.pdf", version.getValue().getOriginalName());
        assertEquals("stored-key.pdf", version.getValue().getStorageKey());
        assertEquals("abc123", version.getValue().getSha256());
        assertEquals(student, version.getValue().getUploadedBy());

        ArgumentCaptor<PlagiarismReport> turnitin = ArgumentCaptor.forClass(PlagiarismReport.class);
        verify(plagiarism).save(turnitin.capture());
        assertEquals(new BigDecimal("12.5"), turnitin.getValue().getSimilarityPercent());
        assertEquals("similarity.pdf", turnitin.getValue().getSimilarityOriginalName());
        assertEquals("ai.pdf", turnitin.getValue().getAiOriginalName());

        verify(storage, times(3)).store(any(), anyString());
        assertEquals(PHASE, view.phase());
        assertEquals(ReportStatus.SUBMITTED, view.status());
        assertFalse(view.locked());
    }

    @Test
    void submit_resubmission_createsNextVersionAndKeepsTheOldOne() {
        Report existing = existingReport(ReportStatus.SUBMITTED);
        when(versions.findFirstByReport_IdOrderByVersionNoDesc(any())).thenReturn(Optional.of(version(existing, 2)));

        submit(new BigDecimal("9"));

        ArgumentCaptor<ReportVersion> version = ArgumentCaptor.forClass(ReportVersion.class);
        verify(versions).save(version.capture());
        assertEquals(3, version.getValue().getVersionNo());
        verify(versions, never()).delete(any(ReportVersion.class));
        verify(versions, never()).deleteById(anyLong());
    }

    @Test
    void submit_afterRevisionRequired_putsTheReportBackToSubmitted() {
        Report existing = existingReport(ReportStatus.REVISION_REQUIRED);
        when(versions.findFirstByReport_IdOrderByVersionNoDesc(any())).thenReturn(Optional.of(version(existing, 1)));

        ReportView view = submit(new BigDecimal("9"));

        assertEquals(ReportStatus.SUBMITTED, existing.getStatus());
        assertEquals(ReportStatus.SUBMITTED, view.status());
    }

    @Test
    void submit_fileNameWithFolders_isStoredWithTheFileNameOnly() {
        MockMultipartFile named = new MockMultipartFile("report", "C:\\docs\\final report.pdf",
                "application/pdf", "%PDF-1.4 x".getBytes());

        service.submit(STUDENT_ID, GROUP_ID, PHASE, new BigDecimal("10"), named, pdf("similarity"), pdf("ai"));

        ArgumentCaptor<ReportVersion> version = ArgumentCaptor.forClass(ReportVersion.class);
        verify(versions).save(version.capture());
        assertEquals("final report.pdf", version.getValue().getOriginalName());
    }

    // ---------- history ----------

    @Test
    void history_groupMember_seesTheReports() {
        Report r = new Report(group, PHASE);
        ReflectionTestUtils.setField(r, "id", 100L);
        when(reports.findByGroup_Id(GROUP_ID)).thenReturn(List.of(r));

        List<ReportView> result = service.history(STUDENT_ID, GROUP_ID);

        assertEquals(1, result.size());
        assertEquals(PHASE, result.get(0).phase());
    }

    @Test
    void history_coordinatorWhoIsNotInTheGroup_isAllowed() {
        AppUser coordinator = user(9L, "Coordinator", Role.COORDINATOR);
        when(users.findById(9L)).thenReturn(Optional.of(coordinator));
        Report r = new Report(group, PHASE);
        when(reports.findByGroup_Id(GROUP_ID)).thenReturn(List.of(r));

        assertEquals(1, service.history(9L, GROUP_ID).size());
    }

    @Test
    void history_otherStudent_is403() {
        AppUser other = user(77L, "Other Student", Role.STUDENT);
        when(users.findById(77L)).thenReturn(Optional.of(other));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.history(77L, GROUP_ID));

        assertEquals(403, e.getStatusCode().value());
    }

    @Test
    void history_unknownGroup_is404() {
        when(groups.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.history(STUDENT_ID, 999L));

        assertEquals(404, e.getStatusCode().value());
    }

    // ---------- download ----------

    private ReportVersion storedVersion() {
        Report r = new Report(group, PHASE);
        ReflectionTestUtils.setField(r, "id", 100L);
        ReportVersion v = version(r, 1);
        when(versions.findById(50L)).thenReturn(Optional.of(v));
        when(storage.load(anyString())).thenAnswer(inv -> new ByteArrayResource(new byte[] {1}));
        return v;
    }

    @Test
    void download_report_returnsTheReportFileWithItsOriginalName() {
        storedVersion();

        DownloadFile file = service.download(STUDENT_ID, 50L, FileKind.REPORT);

        assertEquals("report-v1.pdf", file.fileName());
        verify(storage).load("report-key-1.pdf");
    }

    @Test
    void download_similarityAndAi_returnTheTurnitinFiles() {
        ReportVersion v = storedVersion();
        PlagiarismReport p = new PlagiarismReport(v, new BigDecimal("8"),
                "sim-original.pdf", "sim-key.pdf", "ai-original.pdf", "ai-key.pdf");
        when(plagiarism.findByVersion_Id(50L)).thenReturn(Optional.of(p));

        DownloadFile similarity = service.download(STUDENT_ID, 50L, FileKind.SIMILARITY);
        DownloadFile ai = service.download(STUDENT_ID, 50L, FileKind.AI);

        assertEquals("sim-original.pdf", similarity.fileName());
        assertEquals("ai-original.pdf", ai.fileName());
        verify(storage).load("sim-key.pdf");
        verify(storage).load("ai-key.pdf");
    }

    @Test
    void download_otherStudent_is403AndNothingIsRead() {
        storedVersion();
        AppUser other = user(77L, "Other Student", Role.STUDENT);
        when(users.findById(77L)).thenReturn(Optional.of(other));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.download(77L, 50L, FileKind.REPORT));

        assertEquals(403, e.getStatusCode().value());
        verify(storage, never()).load(anyString());
    }

    @Test
    void download_unknownVersion_is404() {
        when(versions.findById(404L)).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.download(STUDENT_ID, 404L, FileKind.REPORT));

        assertEquals(404, e.getStatusCode().value());
    }

    @Test
    void download_turnitinFilesMissing_is404() {
        storedVersion();

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.download(STUDENT_ID, 50L, FileKind.AI));

        assertEquals(404, e.getStatusCode().value());
    }
}
