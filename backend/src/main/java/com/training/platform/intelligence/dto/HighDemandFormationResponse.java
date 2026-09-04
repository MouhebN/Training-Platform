package com.training.platform.intelligence.dto;

public record HighDemandFormationResponse(
        Long formationId,
        String formationTitle,
        String categoryName,
        int demandScore,
        long learnersInterestedCount,
        long availableSessionCount,
        long confirmedEnrollmentCount,
        long waitlistedEnrollmentCount,
        String reason,
        String suggestedAction
) {
}
