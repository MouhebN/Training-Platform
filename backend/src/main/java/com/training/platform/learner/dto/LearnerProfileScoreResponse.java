package com.training.platform.learner.dto;

import java.util.List;

public record LearnerProfileScoreResponse(
        int score,
        List<String> completedFields,
        List<String> missingFields,
        String message
) {
}
