package com.thesismate.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StudentRecordResponse(
        String studentId, int creditsCompleted, BigDecimal cgpa, LocalDateTime importedAt) {
}