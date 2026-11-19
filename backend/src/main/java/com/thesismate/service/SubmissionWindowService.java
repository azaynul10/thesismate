package com.thesismate.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.thesismate.dto.DeliverableRequirementRequest;
import com.thesismate.dto.DeliverableRequirementView;
import com.thesismate.dto.ExtensionRequest;
import com.thesismate.dto.ExtensionView;
import com.thesismate.dto.SubmissionWindowRequest;
import com.thesismate.dto.SubmissionWindowView;
import com.thesismate.model.AppUser;
import com.thesismate.model.DeliverableRequirement;
import com.thesismate.model.LatePolicy;
import com.thesismate.model.Phase;
import com.thesismate.model.StudentGroup;
import com.thesismate.model.SubmissionExtension;
import com.thesismate.model.SubmissionWindow;
import com.thesismate.repository.AppUserRepository;
import com.thesismate.repository.DeliverableRequirementRepository;
import com.thesismate.repository.StudentGroupRepository;
import com.thesismate.repository.SubmissionExtensionRepository;
import com.thesismate.repository.SubmissionWindowRepository;

/** F15: submission windows, track deliverable checklist, per-group extensions and the "can this group submit now?" rule. */
@Service
public class SubmissionWindowService {

    /** Answer to "may this group submit this phase right now?". F11 uses this before accepting an upload. */
    public record SubmissionCheck(boolean allowed, boolean late, String reason) {
    }

    private final SubmissionWindowRepository windows;
    private final DeliverableRequirementRepository requirements;
    private final SubmissionExtensionRepository extensions;
    private final StudentGroupRepository groups;
    private final AppUserRepository users;

    public SubmissionWindowService(SubmissionWindowRepository windows,
                                   DeliverableRequirementRepository requirements,
                                   SubmissionExtensionRepository extensions,
                                   StudentGroupRepository groups,
                                   AppUserRepository users) {
        this.windows = windows;
        this.requirements = requirements;
        this.extensions = extensions;
        this.groups = groups;
        this.users = users;
    }

    // ---------- windows ----------

    @Transactional
    public SubmissionWindowView createWindow(String coordinatorEmail, SubmissionWindowRequest req) {
        String semester = req.semester().trim();
        validateTimes(req.opensAt(), req.closesAt(), req.latePolicy(), req.lateUntil());
        if (windows.existsBySemesterAndPhaseAndTrack(semester, req.phase(), req.track())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A window for this semester, phase and track already exists");
        }
        AppUser coordinator = findUser(coordinatorEmail);
        SubmissionWindow w = windows.save(new SubmissionWindow(semester, req.phase(), req.track(),
                req.opensAt(), req.closesAt(), req.latePolicy(), req.lateUntil(), coordinator));
        return toView(w);
    }

    /** Semester, phase and track are fixed; only the times and late policy change. */
    @Transactional
    public SubmissionWindowView updateWindow(Long id, SubmissionWindowRequest req) {
        SubmissionWindow w = windows.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Window not found"));
        validateTimes(req.opensAt(), req.closesAt(), req.latePolicy(), req.lateUntil());
        w.reschedule(req.opensAt(), req.closesAt(), req.latePolicy(), req.lateUntil());
        return toView(windows.save(w));
    }

    @Transactional(readOnly = true)
    public List<SubmissionWindowView> listWindows() {
        return windows.findAllByOrderBySemesterDescPhaseAscTrackAsc().stream().map(this::toView).toList();
    }

    // ---------- deliverable checklist ----------

    @Transactional
    public DeliverableRequirementView addRequirement(String coordinatorEmail, DeliverableRequirementRequest req) {
        if (requirements.findByPhaseAndTrackAndDeliverableType(req.phase(), req.track(), req.deliverableType())
                .isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This deliverable is already on the checklist for that phase and track");
        }
        boolean mandatory = req.mandatory() == null || req.mandatory();
        AppUser coordinator = findUser(coordinatorEmail);
        DeliverableRequirement r = requirements.save(new DeliverableRequirement(
                req.phase(), req.track(), req.deliverableType(), mandatory, coordinator));
        return toView(r);
    }

    @Transactional(readOnly = true)
    public List<DeliverableRequirementView> listRequirements() {
        return requirements.findAllByOrderByPhaseAscTrackAscDeliverableTypeAsc().stream()
                .map(this::toView).toList();
    }

    @Transactional
    public void deleteRequirement(Long id) {
        if (!requirements.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Requirement not found");
        }
        requirements.deleteById(id);
    }

    // ---------- extensions ----------

    /** One extension per group and window; granting again updates it. A reason is required. */
    @Transactional
    public ExtensionView grantExtension(String coordinatorEmail, ExtensionRequest req) {
        String reason = req.reason().trim();
        if (reason.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A reason is required for an extension");
        }
        SubmissionWindow window = windows.findById(req.windowId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Window not found"));
        StudentGroup group = groups.findById(req.groupId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        if (group.getTrack() != window.getTrack() || !group.getSemester().equals(window.getSemester())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The group does not belong to this window's track and semester");
        }
        if (!req.newClosesAt().isAfter(window.getClosesAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The new closing time must be after the window's closing time");
        }
        AppUser coordinator = findUser(coordinatorEmail);
        SubmissionExtension ext = extensions.findByGroup_IdAndWindow_Id(group.getId(), window.getId())
                .map(existing -> {
                    existing.update(req.newClosesAt(), reason, coordinator);
                    return existing;
                })
                .orElseGet(() -> new SubmissionExtension(group, window, req.newClosesAt(), reason, coordinator));
        return toView(extensions.save(ext));
    }

    @Transactional(readOnly = true)
    public List<ExtensionView> listExtensions(Long windowId) {
        return extensions.findByWindow_Id(windowId).stream().map(this::toView).toList();
    }

    // ---------- the rule F11 will call ----------

    /**
     * May this group submit this phase at "now"?
     * Open from opensAt to the closing time (the group's extension if it has one, else the window's).
     * After that, only if the policy is LATE_ALLOWED and now is before lateUntil (the submission is then late).
     */
    @Transactional(readOnly = true)
    public SubmissionCheck checkSubmission(Long groupId, Phase phase, LocalDateTime now) {
        StudentGroup group = groups.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        SubmissionWindow w = windows.findBySemesterAndPhaseAndTrack(group.getSemester(), phase, group.getTrack())
                .orElse(null);
        if (w == null) {
            return new SubmissionCheck(false, false, "No submission window has been set for this phase");
        }
        if (now.isBefore(w.getOpensAt())) {
            return new SubmissionCheck(false, false, "The submission window has not opened yet");
        }
        LocalDateTime closes = extensions.findByGroup_IdAndWindow_Id(groupId, w.getId())
                .map(SubmissionExtension::getNewClosesAt)
                .filter(c -> c.isAfter(w.getClosesAt()))
                .orElse(w.getClosesAt());
        if (!now.isAfter(closes)) {
            return new SubmissionCheck(true, false, "Window is open");
        }
        if (w.getLatePolicy() == LatePolicy.LATE_ALLOWED && w.getLateUntil() != null
                && !now.isAfter(w.getLateUntil())) {
            return new SubmissionCheck(true, true, "Window is closed; late submission is allowed");
        }
        return new SubmissionCheck(false, false, "The submission window has closed");
    }

    // ---------- helpers ----------

    private void validateTimes(LocalDateTime opensAt, LocalDateTime closesAt, LatePolicy policy, LocalDateTime lateUntil) {
        if (!closesAt.isAfter(opensAt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The window must close after it opens");
        }
        if (policy == LatePolicy.NO_LATE && lateUntil != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lateUntil is only allowed when late submission is allowed");
        }
        if (policy == LatePolicy.LATE_ALLOWED && (lateUntil == null || !lateUntil.isAfter(closesAt))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Late submission needs a lateUntil time after the closing time");
        }
    }

    private AppUser findUser(String email) {
        return users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in"));
    }

    private SubmissionWindowView toView(SubmissionWindow w) {
        return new SubmissionWindowView(w.getId(), w.getSemester(), w.getPhase(), w.getTrack(),
                w.getOpensAt(), w.getClosesAt(), w.getLatePolicy(), w.getLateUntil(),
                w.getCreatedBy().getFullName(), w.getCreatedAt());
    }

    private DeliverableRequirementView toView(DeliverableRequirement r) {
        return new DeliverableRequirementView(r.getId(), r.getPhase(), r.getTrack(),
                r.getDeliverableType(), r.isMandatory());
    }

    private ExtensionView toView(SubmissionExtension e) {
        return new ExtensionView(e.getId(), e.getGroup().getId(), e.getWindow().getId(),
                e.getNewClosesAt(), e.getReason(), e.getGrantedBy().getFullName(), e.getGrantedAt());
    }
}
