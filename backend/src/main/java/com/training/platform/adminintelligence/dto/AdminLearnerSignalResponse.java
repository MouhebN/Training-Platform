package com.training.platform.adminintelligence.dto;

public record AdminLearnerSignalResponse(
        Long learnerId,
        String learnerFullName,
        String email,
        int score,
        String message
) {
}
