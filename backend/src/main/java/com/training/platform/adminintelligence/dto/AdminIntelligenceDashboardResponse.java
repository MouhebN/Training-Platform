package com.training.platform.adminintelligence.dto;

import java.time.Instant;
import java.util.List;

public record AdminIntelligenceDashboardResponse(
        Instant generatedAt,
        List<AdminSmartCardResponse> smartCards,
        List<AdminAlertResponse> alerts,
        List<AdminRecommendedActionResponse> recommendedActions,
        List<AdminFormationDemandResponse> highDemandFormations,
        List<AdminFormationDemandResponse> formationsWithoutOpenSessions,
        List<AdminSessionCapacityResponse> fullOrAlmostFullSessions,
        List<AdminTrainerLoadResponse> overloadedTrainers,
        List<AdminLearnerSignalResponse> learnersCloseToCertification,
        List<AdminLearnerSignalResponse> learnersWithIncompleteProfiles
) {
}
