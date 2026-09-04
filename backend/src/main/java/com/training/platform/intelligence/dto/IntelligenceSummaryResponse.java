package com.training.platform.intelligence.dto;

public record IntelligenceSummaryResponse(
        long totalActiveFormations,
        long totalOpenSessions,
        long totalConfirmedEnrollments,
        long totalWaitlistedEnrollments,
        int overloadedTrainerCount,
        int highRiskSessionCount,
        int incompleteLearnerProfileCount,
        int highDemandFormationCount
) {
}
