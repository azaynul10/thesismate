package com.thesismate.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One uploaded version of a report, with its Turnitin data. */
public record ReportVersionView(
        Long id,
        int versionNo,
        String originalName,
        long sizeBytes,
        String sha256,
        boolean late,
        boolean locked,
        String uploadedByName,
        LocalDateTime uploadedAt,
        BigDecimal similarityPercent,
        String similarityFileName,
        String aiFileName) {
}
