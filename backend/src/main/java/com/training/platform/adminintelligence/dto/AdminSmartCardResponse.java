package com.training.platform.adminintelligence.dto;

public record AdminSmartCardResponse(
        String key,
        String title,
        String value,
        String subtitle,
        AdminIntelligenceSeverity severity
) {
}
