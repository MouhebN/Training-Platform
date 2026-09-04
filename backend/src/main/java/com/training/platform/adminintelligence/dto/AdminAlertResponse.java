package com.training.platform.adminintelligence.dto;

public record AdminAlertResponse(
        String type,
        AdminIntelligenceSeverity severity,
        String title,
        String message,
        Long relatedId,
        String relatedLabel
) {
}
