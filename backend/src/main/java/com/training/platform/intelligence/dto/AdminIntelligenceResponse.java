package com.training.platform.intelligence.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminIntelligenceResponse(
        LocalDateTime generatedAt,
        Integer globalHealthScore,
        IntelligenceSummaryResponse summary,
        List<IntelligenceAlertResponse> alerts,
        List<HighDemandFormationResponse> highDemandFormations,
        List<TrainerWorkloadInsightResponse> overloadedTrainers,
        List<SessionRiskInsightResponse> sessionRisks,
        List<LearnerProfileRiskResponse> learnerProfileRisks,
        List<MissingSkillInsightResponse> topMissingSkills,
        List<RecommendedActionResponse> recommendedActions
) {
}
