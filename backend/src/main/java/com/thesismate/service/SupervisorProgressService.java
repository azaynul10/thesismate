package com.thesismate.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thesismate.dto.GroupProgressView;
import com.thesismate.dto.ProgressSummary;
import com.thesismate.model.Allocation;
import com.thesismate.repository.AllocationRepository;
import com.thesismate.repository.GroupMemberRepository;
import com.thesismate.service.WeeklyReportService.WeeklySummary;

/**
 * F16 + F17 for supervisors: one row per supervised group with milestone progress and weekly report status.
 * The groups come from the F7 allocations, so a group appears here as soon as a request is accepted.
 */
@Service
public class SupervisorProgressService {

    private final AllocationRepository allocations;
    private final GroupMemberRepository members;
    private final MilestoneService milestones;
    private final WeeklyReportService weekly;

    public SupervisorProgressService(AllocationRepository allocations, GroupMemberRepository members,
                                     MilestoneService milestones, WeeklyReportService weekly) {
        this.allocations = allocations;
        this.members = members;
        this.milestones = milestones;
        this.weekly = weekly;
    }

    @Transactional(readOnly = true)
    public List<GroupProgressView> overview(Long supervisorId) {
        List<Allocation> mine = allocations.findBySupervisorIdAndStatus(supervisorId, Allocation.Status.ACTIVE);
        List<Long> groupIds = mine.stream().map(a -> a.getGroup().getId()).distinct().toList();
        Map<Long, ProgressSummary> progress = milestones.progressOf(groupIds);
        return mine.stream().map(a -> {
            Long groupId = a.getGroup().getId();
            WeeklySummary w = weekly.summaryOf(a.getGroup());
            List<String> names = members.findByGroupIdAndActiveTrue(groupId).stream()
                    .map(m -> m.getMember().getFullName()).toList();
            return new GroupProgressView(groupId, a.getGroup().getTrack(), a.getGroup().getSemester(),
                    a.getSupervisorRole(), names, progress.get(groupId), w.applicable(),
                    w.missingWeeks(), w.pendingReports(), w.lastReportWeek());
        }).toList();
    }
}
