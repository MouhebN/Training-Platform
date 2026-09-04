package com.training.platform.intelligence.dto;

public record SessionRiskInsightResponse(
        Long sessionId,
        String sessionTitle,
        String formationTitle,
        int capacity,
        long confirmedEnrollments,
        long waitlistedEnrollments,
        int capacityUsagePercentage,
        RiskLevel riskLevel,
        String reason,
        String suggestedAction
) {
}
