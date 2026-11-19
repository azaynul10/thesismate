package com.thesismate.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.thesismate.dto.ReportVersionView;
import com.thesismate.dto.ReportView;
import com.thesismate.model.AppUser;
import com.thesismate.model.Phase;
import com.thesismate.model.PlagiarismReport;
import com.thesismate.model.Report;
import com.thesismate.model.ReportVersion;
import com.thesismate.model.Role;
import com.thesismate.model.StudentGroup;
import com.thesismate.repository.AppUserRepository;
import com.thesismate.repository.GroupMemberRepository;
import com.thesismate.repository.PlagiarismReportRepository;
import com.thesismate.repository.ReportRepository;
import com.thesismate.repository.ReportVersionRepository;
import com.thesismate.repository.StudentGroupRepository;
import com.thesismate.service.FileStorageService.StoredFile;
import com.thesismate.service.SubmissionWindowService.SubmissionCheck;

/**
 * F11: phase report submission with version history.
 * A group member uploads the report PDF together with its Turnitin similarity report, the Turnitin AI report and the
 * similarity percentage. The upload is accepted only inside an open window (F15), with similarity at most 15 percent,
 * and never for an approved report. Every accepted upload becomes a new version; older versions are kept.
 */
@Service
public class ReportService {

    /** Highest Turnitin similarity (in percent) that may be submitted. */
    public static final BigDecimal MAX_SIMILARITY = new BigDecimal("15");

    /** Which of the three stored files of a version to download. */
    public enum FileKind { REPORT, SIMILARITY, AI }

    /** A stored file plus the name it was uploaded with. */
    public record DownloadFile(Resource resource, String fileName) {
    }

    private final ReportRepository reports;
    private final ReportVersionRepository versions;
    private final PlagiarismReportRepository plagiarism;
    private final StudentGroupRepository groups;
    private final GroupMemberRepository members;
    private final AppUserRepository users;
    private final SubmissionWindowService windows;
    private final FileStorageService storage;

    public ReportService(ReportRepository reports, ReportVersionRepository versions,
                         PlagiarismReportRepository plagiarism, StudentGroupRepository groups,
                         GroupMemberRepository members, AppUserRepository users,
                         SubmissionWindowService windows, FileStorageService storage) {
        this.reports = reports;
        this.versions = versions;
        this.plagiarism = plagiarism;
        this.groups = groups;
        this.members = members;
        this.users = users;
        this.windows = windows;
        this.storage = storage;
    }

    // ---------- submit ----------

    @Transactional
    public ReportView submit(Long userId, Long groupId, Phase phase, BigDecimal similarityPercent,
                             MultipartFile report, MultipartFile similarityReport, MultipartFile aiReport) {
        AppUser user = findUser(userId);
        StudentGroup group = findGroup(groupId);
        requireMember(userId, groupId);
        if (group.getStatus() != StudentGroup.Status.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This group has been disbanded");
        }
        if (phase == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose the phase of the report");
        }

        // F15 rule: is the window open for this group (own extension included) or open for late submission?
        SubmissionCheck check = windows.checkSubmission(groupId, phase, LocalDateTime.now());
        if (!check.allowed()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, check.reason());
        }

        // Gate: Turnitin similarity at most 15 percent, with both Turnitin files attached.
        if (similarityPercent == null || similarityPercent.signum() < 0
                || similarityPercent.compareTo(new BigDecimal("100")) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Enter the Turnitin similarity percentage (0 to 100)");
        }
        if (similarityPercent.compareTo(MAX_SIMILARITY) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Turnitin similarity is " + similarityPercent.stripTrailingZeros().toPlainString()
                            + "%, the limit is " + MAX_SIMILARITY.toPlainString() + "%");
        }
        requireFile(report, "The report PDF");
        requireFile(similarityReport, "The Turnitin similarity report");
        requireFile(aiReport, "The Turnitin AI report");
        storage.validatePdf(report, "The report PDF");
        storage.validatePdf(similarityReport, "The Turnitin similarity report");
        storage.validatePdf(aiReport, "The Turnitin AI report");

        Report existing = reports.findByGroup_IdAndPhase(groupId, phase).orElse(null);
        if (existing != null && existing.isLocked()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An approved report cannot be replaced");
        }
        Report target = existing != null ? existing : reports.save(new Report(group, phase));

        int versionNo = versions.findFirstByReport_IdOrderByVersionNoDesc(target.getId())
                .map(v -> v.getVersionNo() + 1).orElse(1);

        StoredFile reportFile = storage.store(report, "The report PDF");
        StoredFile similarityFile = storage.store(similarityReport, "The Turnitin similarity report");
        StoredFile aiFile = storage.store(aiReport, "The Turnitin AI report");

        ReportVersion version = versions.save(new ReportVersion(target, versionNo, cleanName(report),
                reportFile.key(), reportFile.size(), reportFile.sha256(), check.late(), user));
        plagiarism.save(new PlagiarismReport(version, similarityPercent,
                cleanName(similarityReport), similarityFile.key(), cleanName(aiReport), aiFile.key()));

        target.markSubmitted();
        reports.save(target);
        return toView(target);
    }

    // ---------- read ----------

    /** All reports of a group with their versions. Group members and the coordinator may read them. */
    @Transactional(readOnly = true)
    public List<ReportView> history(Long userId, Long groupId) {
        AppUser user = findUser(userId);
        findGroup(groupId);
        requireMemberOrCoordinator(user, groupId);
        return reports.findByGroup_Id(groupId).stream()
                .sorted(Comparator.comparing(Report::getPhase))
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public DownloadFile download(Long userId, Long versionId, FileKind kind) {
        AppUser user = findUser(userId);
        ReportVersion version = versions.findById(versionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Version not found"));
        requireMemberOrCoordinator(user, version.getReport().getGroup().getId());
        if (kind == FileKind.REPORT) {
            return new DownloadFile(storage.load(version.getStorageKey()), version.getOriginalName());
        }
        PlagiarismReport p = plagiarism.findByVersion_Id(versionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turnitin files not found"));
        if (kind == FileKind.SIMILARITY) {
            return new DownloadFile(storage.load(p.getSimilarityStorageKey()), p.getSimilarityOriginalName());
        }
        return new DownloadFile(storage.load(p.getAiStorageKey()), p.getAiOriginalName());
    }

    // ---------- helpers ----------

    private void requireFile(MultipartFile file, String label) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " is required");
        }
    }

    private void requireMember(Long userId, Long groupId) {
        boolean member = members.findByGroupIdAndActiveTrue(groupId).stream()
                .anyMatch(m -> m.getMember().getId().equals(userId));
        if (!member) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this group");
        }
    }

    private void requireMemberOrCoordinator(AppUser user, Long groupId) {
        if (user.getRole() == Role.COORDINATOR) {
            return;
        }
        requireMember(user.getId(), groupId);
    }

    private AppUser findUser(Long userId) {
        return users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in"));
    }

    private StudentGroup findGroup(Long groupId) {
        return groups.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
    }

    /** Keeps only the file name (no folders) and at most 255 characters. */
    private String cleanName(MultipartFile file) {
        String name = file.getOriginalFilename() == null ? "file.pdf" : file.getOriginalFilename();
        name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1).trim();
        if (name.isEmpty()) {
            name = "file.pdf";
        }
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    private ReportView toView(Report r) {
        List<ReportVersionView> list = versions.findByReport_IdOrderByVersionNoDesc(r.getId()).stream()
                .map(this::toView).toList();
        return new ReportView(r.getId(), r.getGroup().getId(), r.getPhase(), r.getStatus(), r.isLocked(), list);
    }

    private ReportVersionView toView(ReportVersion v) {
        PlagiarismReport p = plagiarism.findByVersion_Id(v.getId()).orElse(null);
        return new ReportVersionView(v.getId(), v.getVersionNo(), v.getOriginalName(), v.getSizeBytes(),
                v.getSha256(), v.isLate(), v.isLocked(), v.getUploadedBy().getFullName(), v.getUploadedAt(),
                p == null ? null : p.getSimilarityPercent(),
                p == null ? null : p.getSimilarityOriginalName(),
                p == null ? null : p.getAiOriginalName());
    }
}
