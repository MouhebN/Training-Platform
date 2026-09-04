package com.training.platform.learningpath.dto;

import com.training.platform.learner.entity.LearnerLevel;
import java.util.List;

public record LearningPathResponse(
        Long learnerId,
        String learnerFullName,
        String goal,
        LearnerLevel currentLevel,
        int globalProgressPercentage,
        int estimatedTotalHours,
        int completedSteps,
        int totalSteps,
        Long nextRecommendedFormationId,
        String nextRecommendedFormationTitle,
        List<LearningPathStepResponse> steps
) {
}
