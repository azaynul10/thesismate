package com.thesismate.dto;

import java.util.List;

import com.thesismate.model.Phase;
import com.thesismate.model.ReportStatus;

/** A group's report for one phase with its full version history (newest version first). */
public record ReportView(
        Long id,
        Long groupId,
        Phase phase,
        ReportStatus status,
        boolean locked,
        List<ReportVersionView> versions) {
}
