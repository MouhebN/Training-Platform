package com.training.platform.planning.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SessionPlanningSuggestionResponse(
        Long trainerId,
        String trainerFullName,
        String trainerEmail,
        LocalDateTime suggestedStartDate,
        LocalDateTime suggestedEndDate,
        int score,
        WorkloadLevel workloadLevel,
        int expertiseMatchPercentage,
        boolean availabilityMatch,
        boolean conflictFree,
        List<String> reasons,
        List<String> warnings
) {
}
