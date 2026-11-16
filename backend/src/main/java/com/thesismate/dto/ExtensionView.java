package com.thesismate.dto;

import java.time.LocalDateTime;

public record ExtensionView(
        Long id, Long groupId, Long windowId, LocalDateTime newClosesAt,
        String reason, String grantedByName, LocalDateTime grantedAt) {
}
