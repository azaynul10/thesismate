package com.thesismate.dto;

import java.util.List;

public record SupervisorPageResponse(
        List<SupervisorDirectoryItem> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious) {
}
