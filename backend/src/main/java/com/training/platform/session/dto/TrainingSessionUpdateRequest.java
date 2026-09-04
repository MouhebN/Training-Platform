package com.training.platform.session.dto;

import com.training.platform.session.entity.SessionStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record TrainingSessionUpdateRequest(
        @NotNull(message = "Formation is required")
        Long formationId,

        @NotNull(message = "Trainer is required")
        Long trainerId,

        String title,
        String description,

        @NotNull(message = "Start date is required")
        LocalDateTime startDate,

        @NotNull(message = "End date is required")
        LocalDateTime endDate,

        @NotNull(message = "Capacity is required")
        @Min(value = 1, message = "Capacity must be greater than 0")
        Integer capacity,

        String location,
        Boolean online,
        String meetingUrl,
        SessionStatus status
) {
}
