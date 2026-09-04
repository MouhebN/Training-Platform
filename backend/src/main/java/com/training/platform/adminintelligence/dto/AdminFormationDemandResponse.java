package com.training.platform.adminintelligence.dto;

public record AdminFormationDemandResponse(
        Long formationId,
        String formationTitle,
        String categoryName,
        long enrollmentCount,
        long waitlistedCount,
        long openSessionCount
) {
}
