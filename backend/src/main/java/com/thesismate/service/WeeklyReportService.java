package com.thesismate.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.thesismate.dto.WeeklyOverviewView;
import com.thesismate.dto.WeeklyReportRequest;
import com.thesismate.dto.WeeklyReportView;
import com.thesismate.event.WeeklyReportReminderEvent;
import com.thesismate.event.WeeklyReportReviewedEvent;
import com.thesismate.event.WeeklyReportSubmittedEvent;
import com.thesismate.model.Allocation;
import com.thesismate.model.AppUser;
import com.thesismate.model.Phase;
import com.thesismate.model.StudentGroup;
import com.thesismate.model.Track;
import com.thesismate.model.WeeklyReport;
import com.thesismate.model.WeeklyReportReminder;
import com.thesismate.model.WeeklyReportStatus;
import com.thesismate.repository.AllocationRepository;
import com.thesismate.repository.GroupMemberRepository;
import com.thesismate.repository.SubmissionWindowRepository;
import com.thesismate.repository.WeeklyReportReminderRepository;
import com.thesismate.repository.WeeklyReportRepository;
import com.thesismate.service.GroupAccessService.Access;

/**
 * F17: weekly progress reports with supervisor sign-off, for Thesis and Project groups.
 * <ul>
 *   <li>A week runs Monday to Sunday. A group may hand in several reports per week (done, next, blockers).
 *       Every report is kept and reviewed on its own.</li>
 *   <li>Reports start in the week the group got its first supervisor (F7 allocation), because someone has to sign off.</li>
 *   <li>Reports stop after the week in which the FINAL submission window (F15) of the group's track closes.</li>
 *   <li>A supervisor or co-supervisor of the group approves a report or returns it with a remark. Approval
 *       never closes a week: the group can keep sending reports.</li>
 *   <li>A past week with no report at all is "missing". A daily job records one reminder per group and missing week.</li>
 * </ul>
 */
@Service
public class WeeklyReportService {

    /** Internship groups keep a logbook with their industry supervisor instead. */
    static final Set<Track> TRACKED_TRACKS = EnumSet.of(Track.THESIS, Track.PROJECT);

    /** Where a group stands in the weekly cycle. */
    record Tracking(boolean applicable, boolean hasSupervisor, LocalDate startWeek,
                    LocalDate lastWeek, LocalDate currentWeek) {

        /** True when at least one week can take a report. */
        boolean hasWeeks() {
            return applicable && hasSupervisor && !startWeek.isAfter(lastWeek);
        }
    }

    /** Numbers for the supervisor progress page. */
    public record WeeklySummary(boolean applicable, int missingWeeks, int pendingReports, LocalDate lastReportWeek) {
    }

    private final WeeklyReportRepository reports;
    private final WeeklyReportReminderRepository reminders;
    private final GroupMemberRepository members;
    private final AllocationRepository allocations;
    private final SubmissionWindowRepository windows;
    private final GroupAccessService access;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public WeeklyReportService(WeeklyReportRepository reports, WeeklyReportReminderRepository reminders,
                               GroupMemberRepository members, AllocationRepository allocations,
                               SubmissionWindowRepository windows, GroupAccessService access,
                               ApplicationEventPublisher events, Clock clock) {
        this.reports = reports;
        this.reminders = reminders;
        this.members = members;
        this.allocations = allocations;
        this.windows = windows;
        this.access = access;
        this.events = events;
        this.clock = clock;
    }

    // ---------- group side ----------

    /** Hand in a report for a week. Without a week the current week is used. A group may send several per week. */
    @Transactional
    public WeeklyReportView submit(Long userId, Long groupId, LocalDate weekStart, WeeklyReportRequest req) {
        Access a = access.requireMember(userId, groupId);
        StudentGroup group = a.group();
        access.requireActive(group);
        if (!TRACKED_TRACKS.contains(group.getTrack())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Weekly reports are for Thesis and Project groups");
        }
        Tracking t = tracking(group);
        if (!t.hasSupervisor()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Weekly reports start once a supervisor has accepted your group");
        }
        LocalDate week = weekStart == null ? t.currentWeek() : weekStart;
        if (!WeekCalendar.isWeekStart(week)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A week must start on a Monday");
        }
        if (week.isAfter(t.currentWeek())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot hand in a report for a future week");
        }
        if (week.isBefore(t.startWeek())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Weekly reports start from the week of " + t.startWeek() + ", when your supervisor was assigned");
        }
        if (week.isAfter(t.lastWeek())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Weekly reports have ended for this semester");
        }
        String done = requireText(req.done(), "Describe what was done this week");
        String next = requireText(req.next(), "Describe what comes next");
        String blockers = req.blockers() == null || req.blockers().isBlank() ? null : req.blockers().trim();

        LocalDateTime now = now();
        WeeklyReport saved = reports.save(new WeeklyReport(group, week, done, next, blockers, a.user(), now));
        events.publishEvent(new WeeklyReportSubmittedEvent(saved.getId(), groupId, week, supervisorIds(groupId)));
        return toView(saved, memberNames(groupId));
    }

    /** The weekly report page of a group. Members, allocated supervisors and the coordinator may read it. */
    @Transactional(readOnly = true)
    public WeeklyOverviewView overview(Long userId, Long groupId) {
        Access a = access.requireReadAccess(userId, groupId);
        StudentGroup group = a.group();
        Tracking t = tracking(group);
        List<WeeklyReport> list = reports.findByGroup_IdOrderByWeekStartDescSubmittedAtDesc(groupId);
        List<String> names = memberNames(groupId);
        List<WeeklyReportView> views = list.stream().map(r -> toView(r, names)).toList();

        List<LocalDate> missing = missingWeeks(t, weeksOf(list));
        boolean currentSubmitted = weeksOf(list).contains(t.currentWeek());
        int returned = (int) list.stream().filter(r -> r.getStatus() == WeeklyReportStatus.RETURNED).count();

        String notice = null;
        if (!t.applicable()) {
            notice = "Weekly progress reports are for Thesis and Project groups. "
                    + "Internship students keep a weekly logbook with their industry supervisor instead.";
        } else if (!t.hasSupervisor()) {
            notice = "Weekly reports start once a supervisor accepts your group. Send a supervision request first.";
        }
        boolean canSubmit = a.isMember() && group.getStatus() == StudentGroup.Status.ACTIVE && t.hasWeeks();
        return new WeeklyOverviewView(groupId, group.getTrack(), t.applicable(), canSubmit, notice,
                t.hasSupervisor() ? t.startWeek() : null, t.currentWeek(), currentSubmitted,
                missing, returned, views);
    }

    // ---------- supervisor side ----------

    /** Reports waiting for sign-off in all groups the supervisor is allocated to, oldest first. */
    @Transactional(readOnly = true)
    public List<WeeklyReportView> pendingForSupervisor(Long supervisorId) {
        List<Long> groupIds = allocations.findBySupervisorIdAndStatus(supervisorId, Allocation.Status.ACTIVE).stream()
                .map(al -> al.getGroup().getId()).distinct().toList();
        if (groupIds.isEmpty()) {
            return List.of();
        }
        return reports.findByGroup_IdInAndStatusOrderBySubmittedAtAsc(groupIds, WeeklyReportStatus.SUBMITTED).stream()
                .map(r -> toView(r, memberNames(r.getGroup().getId())))
                .toList();
    }

    @Transactional
    public WeeklyReportView approve(Long supervisorId, Long reportId, String comment) {
        WeeklyReport report = findReport(reportId);
        Access a = access.requireAllocatedSupervisor(supervisorId, report.getGroup().getId());
        requireSubmitted(report);
        report.approve(a.user(), blankToNull(comment), now());
        WeeklyReport saved = reports.save(report);
        publishReviewed(saved);
        return toView(saved, memberNames(saved.getGroup().getId()));
    }

    @Transactional
    public WeeklyReportView returnReport(Long supervisorId, Long reportId, String reason) {
        WeeklyReport report = findReport(reportId);
        Access a = access.requireAllocatedSupervisor(supervisorId, report.getGroup().getId());
        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Say what needs to change before returning a report");
        }
        requireSubmitted(report);
        report.returnToGroup(a.user(), reason.trim(), now());
        WeeklyReport saved = reports.save(report);
        publishReviewed(saved);
        return toView(saved, memberNames(saved.getGroup().getId()));
    }

    /** Weekly numbers of one group for the supervisor progress page. */
    @Transactional(readOnly = true)
    public WeeklySummary summaryOf(StudentGroup group) {
        Tracking t = tracking(group);
        if (!t.applicable()) {
            return new WeeklySummary(false, 0, 0, null);
        }
        List<WeeklyReport> list = reports.findByGroup_IdOrderByWeekStartDescSubmittedAtDesc(group.getId());
        int pending = (int) list.stream().filter(r -> r.getStatus() == WeeklyReportStatus.SUBMITTED).count();
        LocalDate last = list.isEmpty() ? null : list.get(0).getWeekStart();
        return new WeeklySummary(true, missingWeeks(t, weeksOf(list)).size(), pending, last);
    }

    // ---------- reminders ----------

    /**
     * Finds groups with a missing past week and records one reminder per group and week.
     * Safe to run as often as needed: a week that already has a reminder is skipped.
     * Returns how many reminders were created. F10 turns the published events into notifications.
     */
    @Transactional
    public int sendReminders() {
        int sent = 0;
        for (StudentGroup group : reports.findGroups(StudentGroup.Status.ACTIVE, TRACKED_TRACKS)) {
            Tracking t = tracking(group);
            if (!t.hasWeeks()) {
                continue;
            }
            List<LocalDate> missing = missingWeeks(t, weeksOf(reports.findByGroup_IdOrderByWeekStartDescSubmittedAtDesc(group.getId())));
            if (missing.isEmpty()) {
                continue;
            }
            LocalDate latest = missing.get(missing.size() - 1);
            if (reminders.existsByGroup_IdAndWeekStart(group.getId(), latest)) {
                continue;
            }
            reminders.save(new WeeklyReportReminder(group, latest));
            List<Long> memberIds = members.findByGroupIdAndActiveTrue(group.getId()).stream()
                    .map(m -> m.getMember().getId()).toList();
            events.publishEvent(new WeeklyReportReminderEvent(group.getId(), latest, missing.size(), memberIds));
            sent++;
        }
        return sent;
    }

    // ---------- week rules ----------

    /** Works out which weeks count for this group. */
    Tracking tracking(StudentGroup group) {
        LocalDate currentWeek = WeekCalendar.weekStart(LocalDate.now(clock));
        if (!TRACKED_TRACKS.contains(group.getTrack())) {
            return new Tracking(false, false, currentWeek, currentWeek, currentWeek);
        }
        LocalDateTime first = reports.firstAllocationTime(group.getId());
        if (first == null) {
            return new Tracking(true, false, currentWeek, currentWeek, currentWeek);
        }
        LocalDate startWeek = WeekCalendar.weekStart(first.toLocalDate());
        LocalDate lastWeek = currentWeek;
        LocalDate finalClose = windows.findBySemesterAndPhaseAndTrack(group.getSemester(), Phase.FINAL, group.getTrack())
                .map(w -> WeekCalendar.weekStart(w.getClosesAt().toLocalDate()))
                .orElse(null);
        if (finalClose != null && finalClose.isBefore(lastWeek)) {
            lastWeek = finalClose;
        }
        return new Tracking(true, true, startWeek, lastWeek, currentWeek);
    }

    /** Past weeks (before the current one) inside the tracked range that have no report. Oldest first. */
    List<LocalDate> missingWeeks(Tracking t, Set<LocalDate> submittedWeeks) {
        if (!t.hasWeeks()) {
            return List.of();
        }
        LocalDate lastPast = t.lastWeek().isBefore(t.currentWeek()) ? t.lastWeek() : t.currentWeek().minusWeeks(1);
        List<LocalDate> missing = new ArrayList<>();
        for (LocalDate week : WeekCalendar.weeksBetween(t.startWeek(), lastPast)) {
            if (!submittedWeeks.contains(week)) {
                missing.add(week);
            }
        }
        return missing;
    }

    // ---------- helpers ----------

    private Set<LocalDate> weeksOf(List<WeeklyReport> list) {
        return list.stream().map(WeeklyReport::getWeekStart).collect(Collectors.toSet());
    }

    private void requireSubmitted(WeeklyReport report) {
        if (report.getStatus() != WeeklyReportStatus.SUBMITTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This report is already " + report.getStatus().name().toLowerCase());
        }
    }

    private void publishReviewed(WeeklyReport r) {
        Long groupId = r.getGroup().getId();
        List<Long> memberIds = members.findByGroupIdAndActiveTrue(groupId).stream()
                .map(m -> m.getMember().getId()).toList();
        events.publishEvent(new WeeklyReportReviewedEvent(r.getId(), groupId, r.getWeekStart(),
                r.getStatus(), r.getReviewComment(), memberIds));
    }

    private List<Long> supervisorIds(Long groupId) {
        return allocations.findByGroupIdAndStatus(groupId, Allocation.Status.ACTIVE).stream()
                .map(al -> al.getSupervisor().getId()).toList();
    }

    private List<String> memberNames(Long groupId) {
        return members.findByGroupIdAndActiveTrue(groupId).stream()
                .map(m -> m.getMember().getFullName()).toList();
    }

    private WeeklyReport findReport(Long id) {
        return reports.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
    }

    private String requireText(String text, String message) {
        if (text == null || text.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return text.trim();
    }

    private String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    private WeeklyReportView toView(WeeklyReport r, List<String> memberNames) {
        LocalDate weekEnd = WeekCalendar.weekEnd(r.getWeekStart());
        AppUser reviewer = r.getReviewedBy();
        return new WeeklyReportView(r.getId(), r.getGroup().getId(), r.getGroup().getTrack(),
                r.getGroup().getSemester(), memberNames, r.getWeekStart(), weekEnd, r.getDone(), r.getNextPlan(),
                r.getBlockers(), r.getStatus(),
                r.getSubmittedAt().toLocalDate().isAfter(weekEnd),
                r.getSubmittedBy().getFullName(), r.getSubmittedAt(),
                reviewer == null ? null : reviewer.getFullName(), r.getReviewedAt(), r.getReviewComment());
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
