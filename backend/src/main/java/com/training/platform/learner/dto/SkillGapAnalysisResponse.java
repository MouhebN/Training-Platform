package com.training.platform.learner.dto;

import java.util.List;

public record SkillGapAnalysisResponse(
        Long learnerId,
        Long formationId,
        String formationTitle,
        List<String> learnerSkills,
        List<String> requiredSkills,
        List<String> matchingSkills,
        List<String> missingSkills,
        int matchPercentage,
        boolean ready,
        String recommendationMessage
) {
}
