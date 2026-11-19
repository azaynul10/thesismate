package com.thesismate.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.thesismate.dto.MilestonePlanView;
import com.thesismate.dto.MilestoneRequest;
import com.thesismate.dto.MilestoneView;
import com.thesismate.dto.ProgressSummary;
import com.thesismate.dto.TaskRequest;
import com.thesismate.dto.TaskView;
import com.thesismate.event.MilestoneCompletedEvent;
import com.thesismate.event.TaskAssignedEvent;
import com.thesismate.model.AppUser;
import com.thesismate.model.Milestone;
import com.thesismate.model.MilestoneTask;
import com.thesismate.model.Phase;
import com.thesismate.model.StudentGroup;
import com.thesismate.model.SubmissionWindow;
import com.thesismate.model.TaskSource;
import com.thesismate.model.TaskStatus;
import com.thesismate.repository.GroupMemberRepository;
import com.thesismate.repository.MilestoneRepository;
import com.thesismate.repository.MilestoneTaskRepository;
import com.thesismate.repository.StudentGroupRepository;
import com.thesismate.repository.SubmissionWindowRepository;
import com.thesismate.service.GroupAccessService.Access;
import com.thesismate.service.MilestoneTemplates.Item;
import com.thesismate.service.ProgressCalculator.MilestoneStats;

/**
 * F16: milestone and task planning for a group.
 * Members of an active group build the plan (from the track template or by hand) and update task status.
 * Allocated supervisors and the coordinator can read it. Completion and overdue are calculated, never stored.
 * Other features reach this class through {@link #progressOf} (F20) and {@link #addGeneratedTask} (F18, F24).
 */
@Service
public class MilestoneService {

    private final MilestoneRepository milestones;
    private final MilestoneTaskRepository tasks;
    private final StudentGroupRepository groups;
    private final GroupMemberRepository members;
    private final SubmissionWindowRepository windows;
    private final GroupAccessService access;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public MilestoneService(MilestoneRepository milestones, MilestoneTaskRepository tasks,
                            StudentGroupRepository groups, GroupMemberRepository members,
                            SubmissionWindowRepository windows, GroupAccessService access,
                            ApplicationEventPublisher events, Clock clock) {
        this.milestones = milestones;
        this.tasks = tasks;
        this.groups = groups;
        this.members = members;
        this.windows = windows;
        this.access = access;
        this.events = events;
        this.clock = clock;
    }

    // ---------- read ----------

    @Transactional(readOnly = true)
    public MilestonePlanView plan(Long userId, Long groupId) {
        Access a = access.requireReadAccess(userId, groupId);
        boolean canEdit = a.isMember() && a.group().getStatus() == StudentGroup.Status.ACTIVE;
        return buildPlan(a.group(), canEdit);
    }

    /** Calculated progress of one group. No permission check: for other features' services (F20 analytics). */
    @Transactional(readOnly = true)
    public ProgressSummary progressOf(Long groupId) {
        return progressOf(List.of(groupId)).get(groupId);
    }

    /** Calculated progress of several groups with one task query. Every id is present in the result. */
    @Transactional(readOnly = true)
    public Map<Long, ProgressSummary> progressOf(Collection<Long> groupIds) {
        LocalDate today = today();
        Map<Long, List<MilestoneTask>> tasksByGroup = groupIds.isEmpty() ? Map.of()
                : tasks.findByMilestone_Group_IdIn(groupIds).stream()
                        .collect(Collectors.groupingBy(t -> t.getMilestone().getGroup().getId()));
        Map<Long, ProgressSummary> result = new LinkedHashMap<>();
        for (Long groupId : groupIds) {
            List<Milestone> list = milestones.findByGroup_IdOrderBySortOrderAscIdAsc(groupId);
            result.put(groupId, summarize(list, tasksByGroup.getOrDefault(groupId, List.of()), today));
        }
        return result;
    }

    // ---------- template and milestones ----------

    /** Copies the track template into the plan. Allowed once per group. */
    @Transactional
    public MilestonePlanView applyTemplate(Long userId, Long groupId) {
        Access a = access.requireMember(userId, groupId);
        // Lock the group so two members pressing the button together cannot both create the template.
        StudentGroup group = groups.findByIdForUpdate(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        access.requireActive(group);
        if (milestones.existsByGroup_IdAndFromTemplateTrue(groupId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The template was already added to your plan");
        }
        List<Item> items = MilestoneTemplates.forTrack(group.getTrack());
        List<LocalDate> dueDates = templateDueDates(group, items, today());
        int order = nextSortOrder(groupId);
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            milestones.save(new Milestone(group, item.title(), item.description(), item.phase(),
                    dueDates.get(i), order + i, true, item.key(), a.user()));
        }
        return buildPlan(group, true);
    }

    @Transactional
    public MilestoneView addMilestone(Long userId, Long groupId, MilestoneRequest req) {
        Access a = access.requireMember(userId, groupId);
        access.requireActive(a.group());
        String title = requireTitle(req.title());
        Milestone saved = milestones.save(new Milestone(a.group(), title, clean(req.description()),
                req.phase(), req.dueDate(), nextSortOrder(groupId), false, null, a.user()));
        return milestoneView(saved, List.of(), activeMemberIds(groupId), today());
    }

    @Transactional
    public MilestoneView updateMilestone(Long userId, Long milestoneId, MilestoneRequest req) {
        Milestone m = findMilestone(milestoneId);
        Long groupId = m.getGroup().getId();
        Access a = access.requireMember(userId, groupId);
        access.requireActive(a.group());
        m.update(requireTitle(req.title()), clean(req.description()), req.phase(), req.dueDate());
        milestones.save(m);
        return milestoneView(m, tasks.findByMilestone_Id(milestoneId), activeMemberIds(groupId), today());
    }

    @Transactional
    public void deleteMilestone(Long userId, Long milestoneId) {
        Milestone m = findMilestone(milestoneId);
        Access a = access.requireMember(userId, m.getGroup().getId());
        access.requireActive(a.group());
        tasks.deleteAll(tasks.findByMilestone_Id(milestoneId));
        milestones.delete(m);
    }

    // ---------- tasks ----------

    @Transactional
    public TaskView addTask(Long userId, Long milestoneId, TaskRequest req) {
        Milestone m = findMilestone(milestoneId);
        Long groupId = m.getGroup().getId();
        Access a = access.requireMember(userId, groupId);
        access.requireActive(a.group());
        String title = requireTitle(req.title());
        AppUser assignee = resolveAssignee(groupId, req.assigneeId());
        MilestoneTask saved = tasks.save(new MilestoneTask(m, title, clean(req.description()), assignee,
                req.dueDate(), TaskSource.MANUAL, null, a.user()));
        notifyAssigned(saved, groupId, assignee, a.user().getId());
        return taskView(saved, activeMemberIds(groupId), today());
    }

    @Transactional
    public TaskView updateTask(Long userId, Long taskId, TaskRequest req) {
        MilestoneTask t = findTask(taskId);
        Long groupId = t.getMilestone().getGroup().getId();
        Access a = access.requireMember(userId, groupId);
        access.requireActive(a.group());
        String title = requireTitle(req.title());
        Long oldAssigneeId = t.getAssignee() == null ? null : t.getAssignee().getId();
        AppUser assignee = resolveAssignee(groupId, req.assigneeId());
        t.update(title, clean(req.description()), assignee, req.dueDate(), now());
        tasks.save(t);
        if (assignee != null && !assignee.getId().equals(oldAssigneeId)) {
            notifyAssigned(t, groupId, assignee, a.user().getId());
        }
        return taskView(t, activeMemberIds(groupId), today());
    }

    @Transactional
    public TaskView changeStatus(Long userId, Long taskId, TaskStatus status) {
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status is required");
        }
        MilestoneTask t = findTask(taskId);
        Milestone m = t.getMilestone();
        Long groupId = m.getGroup().getId();
        Access a = access.requireMember(userId, groupId);
        access.requireActive(a.group());

        boolean wasCompleted = isCompleted(tasks.findByMilestone_Id(m.getId()));
        t.changeStatus(status, now());
        tasks.save(t);
        boolean nowCompleted = isCompleted(tasks.findByMilestone_Id(m.getId()));
        if (!wasCompleted && nowCompleted) {
            events.publishEvent(new MilestoneCompletedEvent(m.getId(), groupId, m.getTitle()));
        }
        return taskView(t, activeMemberIds(groupId), today());
    }

    @Transactional
    public void deleteTask(Long userId, Long taskId) {
        MilestoneTask t = findTask(taskId);
        Access a = access.requireMember(userId, t.getMilestone().getGroup().getId());
        access.requireActive(a.group());
        tasks.delete(t);
    }

    // ---------- for other features ----------

    /**
     * Creates a task on behalf of another feature: a failed acceptance test case (F24) or a follow-up action
     * from meeting minutes (F18). No member check, the caller has already done its own. The same
     * (source, sourceRef) pair creates only one task, so calling it twice is safe.
     * Without a milestone the task goes into the group's "Follow-up actions" milestone, created on first use.
     */
    @Transactional
    public TaskView addGeneratedTask(Long groupId, Long milestoneId, String title, String description,
                                     LocalDate dueDate, Long assigneeId, TaskSource source, String sourceRef,
                                     Long createdByUserId) {
        if (source == null || source == TaskSource.MANUAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A generated task needs a source");
        }
        if (sourceRef != null) {
            Optional<MilestoneTask> existing = tasks.findBySourceAndSourceRef(source, sourceRef);
            if (existing.isPresent()) {
                return taskView(existing.get(), activeMemberIds(groupId), today());
            }
        }
        StudentGroup group = access.requireGroup(groupId);
        access.requireActive(group);
        AppUser creator = access.requireUser(createdByUserId);
        Milestone target;
        if (milestoneId != null) {
            target = findMilestone(milestoneId);
            if (!target.getGroup().getId().equals(groupId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "That milestone belongs to another group");
            }
        } else {
            target = milestones.findByGroup_IdAndTemplateKey(groupId, Milestone.FOLLOW_UPS_KEY)
                    .orElseGet(() -> milestones.save(new Milestone(group, "Follow-up actions",
                            "Tasks that came from meetings and demos.", null, null,
                            nextSortOrder(groupId), false, Milestone.FOLLOW_UPS_KEY, creator)));
        }
        AppUser assignee = resolveAssignee(groupId, assigneeId);
        MilestoneTask saved = tasks.save(new MilestoneTask(target, requireTitle(title), clean(description),
                assignee, dueDate, source, sourceRef, creator));
        notifyAssigned(saved, groupId, assignee, creator.getId());
        return taskView(saved, activeMemberIds(groupId), today());
    }

    // ---------- plan building ----------

    private MilestonePlanView buildPlan(StudentGroup group, boolean canEdit) {
        Long groupId = group.getId();
        LocalDate today = today();
        List<Milestone> list = milestones.findByGroup_IdOrderBySortOrderAscIdAsc(groupId);
        List<MilestoneTask> allTasks = tasks.findByMilestone_Group_Id(groupId);
        Map<Long, List<MilestoneTask>> byMilestone = allTasks.stream()
                .collect(Collectors.groupingBy(t -> t.getMilestone().getId()));
        Set<Long> activeIds = activeMemberIds(groupId);

        List<MilestoneView> views = new ArrayList<>();
        for (Milestone m : list) {
            views.add(milestoneView(m, byMilestone.getOrDefault(m.getId(), List.of()), activeIds, today));
        }
        boolean templateApplied = list.stream().anyMatch(Milestone::isFromTemplate);
        return new MilestonePlanView(groupId, group.getTrack(), group.getSemester(), canEdit, templateApplied,
                summarize(list, allTasks, today), views);
    }

    private ProgressSummary summarize(List<Milestone> list, List<MilestoneTask> allTasks, LocalDate today) {
        Map<Long, List<MilestoneTask>> byMilestone = allTasks.stream()
                .collect(Collectors.groupingBy(t -> t.getMilestone().getId()));
        List<MilestoneStats> stats = new ArrayList<>();
        for (Milestone m : list) {
            List<MilestoneTask> ts = byMilestone.getOrDefault(m.getId(), List.of());
            int done = (int) ts.stream().filter(MilestoneTask::isDone).count();
            int overdue = (int) ts.stream()
                    .filter(t -> ProgressCalculator.taskOverdue(t.isDone(), t.getDueDate(), today)).count();
            stats.add(new MilestoneStats(done, ts.size(), overdue, m.getDueDate()));
        }
        return ProgressCalculator.summarize(stats, today);
    }

    private MilestoneView milestoneView(Milestone m, List<MilestoneTask> ts, Set<Long> activeIds, LocalDate today) {
        List<MilestoneTask> sorted = ts.stream()
                .sorted(Comparator.comparing(MilestoneTask::getId))
                .toList();
        int done = (int) sorted.stream().filter(MilestoneTask::isDone).count();
        int overdueTasks = (int) sorted.stream()
                .filter(t -> ProgressCalculator.taskOverdue(t.isDone(), t.getDueDate(), today)).count();
        boolean completed = ProgressCalculator.milestoneCompleted(done, sorted.size());
        boolean started = sorted.stream().anyMatch(t -> t.getStatus() != TaskStatus.TODO);
        String state = completed ? "COMPLETED" : started ? "IN_PROGRESS" : "NOT_STARTED";
        List<TaskView> taskViews = sorted.stream().map(t -> taskView(t, activeIds, today)).toList();
        return new MilestoneView(m.getId(), m.getTitle(), m.getDescription(), m.getPhase(), m.getDueDate(),
                m.getSortOrder(), m.isFromTemplate(), ProgressCalculator.percent(done, sorted.size()),
                sorted.size(), done, overdueTasks,
                ProgressCalculator.milestoneOverdue(completed, m.getDueDate(), today), state, taskViews);
    }

    private TaskView taskView(MilestoneTask t, Set<Long> activeIds, LocalDate today) {
        AppUser assignee = t.getAssignee();
        return new TaskView(t.getId(), t.getMilestone().getId(), t.getTitle(), t.getDescription(),
                assignee == null ? null : assignee.getId(),
                assignee == null ? null : assignee.getFullName(),
                assignee != null && activeIds.contains(assignee.getId()),
                t.getDueDate(), t.getStatus(),
                ProgressCalculator.taskOverdue(t.isDone(), t.getDueDate(), today),
                t.getSource(), t.getSourceRef(), t.getCreatedAt(), t.getCompletedAt());
    }

    /**
     * Due dates for the template milestones, taken from the F15 submission window of the group's track.
     * The milestones of a phase are spread evenly from today (or from the previous phase's deadline) to the
     * phase's closing date, so the last one falls on the closing date. A phase without a window gets no due date.
     */
    private List<LocalDate> templateDueDates(StudentGroup group, List<Item> items, LocalDate today) {
        Map<Phase, List<Integer>> indexesByPhase = new EnumMap<>(Phase.class);
        for (int i = 0; i < items.size(); i++) {
            indexesByPhase.computeIfAbsent(items.get(i).phase(), p -> new ArrayList<>()).add(i);
        }
        LocalDate[] result = new LocalDate[items.size()];
        LocalDate previousClose = today;
        for (Phase phase : Phase.values()) {
            List<Integer> indexes = indexesByPhase.get(phase);
            if (indexes == null) {
                continue;
            }
            Optional<SubmissionWindow> window =
                    windows.findBySemesterAndPhaseAndTrack(group.getSemester(), phase, group.getTrack());
            if (window.isEmpty()) {
                continue;
            }
            LocalDate close = window.get().getClosesAt().toLocalDate();
            LocalDate start = previousClose.isAfter(today) ? previousClose : today;
            int n = indexes.size();
            for (int k = 0; k < n; k++) {
                if (!close.isAfter(start)) {
                    result[indexes.get(k)] = close;
                } else {
                    long days = ChronoUnit.DAYS.between(start, close);
                    result[indexes.get(k)] = start.plusDays(Math.round(days * (k + 1.0) / n));
                }
            }
            previousClose = close;
        }
        return java.util.Arrays.asList(result);
    }

    // ---------- helpers ----------

    private boolean isCompleted(List<MilestoneTask> ts) {
        int done = (int) ts.stream().filter(MilestoneTask::isDone).count();
        return ProgressCalculator.milestoneCompleted(done, ts.size());
    }

    private void notifyAssigned(MilestoneTask task, Long groupId, AppUser assignee, Long actorId) {
        if (assignee != null && !assignee.getId().equals(actorId)) {
            events.publishEvent(new TaskAssignedEvent(task.getId(), groupId, task.getTitle(),
                    assignee.getId(), actorId));
        }
    }

    /** The assignee must be an active member of the group. Null means unassigned. */
    private AppUser resolveAssignee(Long groupId, Long assigneeId) {
        if (assigneeId == null) {
            return null;
        }
        return members.findByGroupIdAndActiveTrue(groupId).stream()
                .map(m -> m.getMember())
                .filter(u -> u.getId().equals(assigneeId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "The assignee must be a member of this group"));
    }

    private Set<Long> activeMemberIds(Long groupId) {
        Set<Long> ids = new HashSet<>();
        members.findByGroupIdAndActiveTrue(groupId).forEach(m -> ids.add(m.getMember().getId()));
        return ids;
    }

    private int nextSortOrder(Long groupId) {
        return milestones.findByGroup_IdOrderBySortOrderAscIdAsc(groupId).stream()
                .mapToInt(Milestone::getSortOrder).max().orElse(-1) + 1;
    }

    private Milestone findMilestone(Long id) {
        return milestones.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Milestone not found"));
    }

    private MilestoneTask findTask(Long id) {
        return tasks.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
    }

    private String requireTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
        }
        String trimmed = title.trim();
        if (trimmed.length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title can have at most 200 characters");
        }
        return trimmed;
    }

    private String clean(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
