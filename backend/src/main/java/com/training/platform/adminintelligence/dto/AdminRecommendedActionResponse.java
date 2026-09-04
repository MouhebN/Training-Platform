package com.training.platform.adminintelligence.dto;

public record AdminRecommendedActionResponse(
        String priority,
        String title,
        String description,
        String actionLabel,
        String targetRoute
) {
}
