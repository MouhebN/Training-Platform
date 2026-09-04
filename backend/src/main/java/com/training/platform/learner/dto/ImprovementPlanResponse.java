package com.training.platform.learner.dto;

import java.util.List;

public record ImprovementPlanResponse(
        Long learnerId,
        int profileScore,
        String suggestionSource,
        String message,
        List<ImprovementSuggestion> suggestions
) {
}
