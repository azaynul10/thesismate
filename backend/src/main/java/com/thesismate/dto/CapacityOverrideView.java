package com.thesismate.dto;

import java.time.LocalDateTime;

public record CapacityOverrideView(
        Long id, String semester, int oldCapacity, int newCapacity,
        String reason, String coordinatorName, LocalDateTime createdAt) {
}
