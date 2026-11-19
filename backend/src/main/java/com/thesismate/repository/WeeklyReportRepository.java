package com.thesismate.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.thesismate.model.StudentGroup;
import com.thesismate.model.Track;
import com.thesismate.model.WeeklyReport;
import com.thesismate.model.WeeklyReportStatus;

public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, Long> {

    List<WeeklyReport> findByGroup_IdOrderByWeekStartDescSubmittedAtDesc(Long groupId);

    List<WeeklyReport> findByGroup_IdInAndStatusOrderBySubmittedAtAsc(Collection<Long> groupIds, WeeklyReportStatus status);

    long countByGroup_IdAndStatus(Long groupId, WeeklyReportStatus status);

    /** The moment the group got its first supervisor (F7 allocation), whatever its status is now. Null if none. */
    @Query("select min(a.createdAt) from Allocation a where a.group.id = :groupId")
    LocalDateTime firstAllocationTime(@Param("groupId") Long groupId);

    /** Groups that should be handing in weekly reports: active groups of the given tracks. */
    @Query("select g from StudentGroup g where g.status = :status and g.track in :tracks")
    List<StudentGroup> findGroups(@Param("status") StudentGroup.Status status, @Param("tracks") Collection<Track> tracks);
}
