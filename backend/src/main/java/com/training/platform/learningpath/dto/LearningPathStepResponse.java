package com.training.platform.learningpath.dto;

import com.training.platform.catalogue.formation.entity.FormationLevel;
import java.util.List;

public record LearningPathStepResponse(
        int order,
        Long formationId,
        String formationTitle,
        String categoryName,
        FormationLevel level,
        Integer durationHours,
        LearningPathStepStatus status,
        int matchPercentage,
        List<String> requiredSkills,
        List<String> matchingSkills,
        List<String> missingSkills,
        boolean hasAvailableSession,
        String reason,
        int formationProgressPercentage,
        int completedSessions,
        int totalSessions
) {
}
