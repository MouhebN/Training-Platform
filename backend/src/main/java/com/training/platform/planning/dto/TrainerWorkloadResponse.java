package com.training.platform.planning.dto;

import java.util.List;

public record TrainerWorkloadResponse(
        Long trainerId,
        String trainerFullName,
        String trainerEmail,
        int sessionCount,
        long totalHours,
        int completedSessions,
        int upcomingSessions,
        WorkloadLevel workloadLevel,
        String recommendation,
        List<TrainerWorkloadSessionItem> sessions
) {
}
