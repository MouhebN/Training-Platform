package com.training.platform.adminintelligence.dto;

import com.training.platform.planning.dto.WorkloadLevel;

public record AdminTrainerLoadResponse(
        Long trainerId,
        String trainerFullName,
        String trainerEmail,
        long next30DaysHours,
        int upcomingSessions,
        WorkloadLevel workloadLevel
) {
}
