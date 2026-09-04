package com.training.platform.planning.dto;

public record SessionConflictItem(
        String type,
        ConflictSeverity severity,
        String message,
        Long relatedSessionId,
        String relatedSessionTitle
) {
}
