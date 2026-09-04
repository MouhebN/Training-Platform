package com.training.platform.trainer.dto;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;

public record TrainerAvailabilityResponse(
        Long id,
        Long trainerId,
        String trainerName,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Instant createdAt,
        Instant updatedAt
) {
}
