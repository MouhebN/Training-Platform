package com.training.platform.intelligence.dto;

public record RecommendedActionResponse(
        ActionPriority priority,
        String title,
        String description,
        String actionType,
        String relatedEntityType,
        Long relatedEntityId,
        String actionLabel
) {
}
