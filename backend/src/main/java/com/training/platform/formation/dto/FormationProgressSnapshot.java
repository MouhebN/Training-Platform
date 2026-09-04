package com.training.platform.formation.dto;

public record FormationProgressSnapshot(
        int totalSessions,
        int completedSessions,
        int progressPercentage,
        boolean formationComplete
) {
    public static FormationProgressSnapshot empty() {
        return new FormationProgressSnapshot(0, 0, 0, false);
    }
}
