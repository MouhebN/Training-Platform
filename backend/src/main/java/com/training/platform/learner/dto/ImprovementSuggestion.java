package com.training.platform.learner.dto;

import com.training.platform.catalogue.formation.entity.FormationLevel;
import java.util.List;

public record ImprovementSuggestion(
        Long formationId,
        String formationTitle,
        String categoryName,
        FormationLevel level,
        int matchPercentage,
        List<String> missingSkills,
        List<String> reasons,
        ImprovementPriority priority,
        int formationProgressPercentage,
        int completedSessions,
        int totalSessions
) {
}
