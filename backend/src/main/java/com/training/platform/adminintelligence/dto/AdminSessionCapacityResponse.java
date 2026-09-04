package com.training.platform.adminintelligence.dto;

public record AdminSessionCapacityResponse(
        Long sessionId,
        String sessionTitle,
        String formationTitle,
        int capacity,
        long confirmedCount,
        int occupancyPercentage,
        boolean full
) {
}
