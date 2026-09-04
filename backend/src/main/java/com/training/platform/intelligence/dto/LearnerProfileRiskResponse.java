package com.training.platform.intelligence.dto;

import java.util.List;

public record LearnerProfileRiskResponse(
        Long learnerId,
        String learnerFullName,
        int profileScore,
        List<String> missingFields,
        String reason,
        String suggestedAction
) {
}
