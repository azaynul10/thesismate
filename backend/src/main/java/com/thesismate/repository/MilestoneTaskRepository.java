package com.thesismate.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thesismate.model.MilestoneTask;
import com.thesismate.model.TaskSource;

public interface MilestoneTaskRepository extends JpaRepository<MilestoneTask, Long> {

    /** All tasks of a group in one query (no per-milestone loading). */
    List<MilestoneTask> findByMilestone_Group_Id(Long groupId);

    List<MilestoneTask> findByMilestone_Group_IdIn(Collection<Long> groupIds);

    List<MilestoneTask> findByMilestone_Id(Long milestoneId);

    /** Used to create a generated task only once for the same origin. */
    Optional<MilestoneTask> findBySourceAndSourceRef(TaskSource source, String sourceRef);
}
