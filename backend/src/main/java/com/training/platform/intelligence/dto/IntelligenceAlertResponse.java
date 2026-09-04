package com.training.platform.intelligence.dto;

public record IntelligenceAlertResponse(
        String type,
        IntelligenceSeverity severity,
        String title,
        String message,
        String relatedEntityType,
        Long relatedEntityId,
        String actionLabel
) {
}
