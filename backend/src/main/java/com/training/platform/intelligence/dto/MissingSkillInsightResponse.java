package com.training.platform.intelligence.dto;

public record MissingSkillInsightResponse(
        Long skillId,
        String skillName,
        long missingCount,
        long relatedFormationCount,
        String reason,
        String suggestedAction
) {
}
