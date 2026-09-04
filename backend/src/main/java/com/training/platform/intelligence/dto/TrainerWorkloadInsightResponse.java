package com.training.platform.intelligence.dto;

import com.training.platform.planning.dto.WorkloadLevel;

public record TrainerWorkloadInsightResponse(
        Long trainerId,
        String trainerFullName,
        String trainerEmail,
        long totalHours,
        int sessionCount,
        WorkloadLevel workloadLevel,
        String reason,
        String suggestedAction
) {
}
