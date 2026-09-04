package com.training.platform.planning.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record SessionConflictCheckRequest(
        @NotNull Long formationId,
        @NotNull Long trainerId,
        @NotNull LocalDateTime startDate,
        @NotNull LocalDateTime endDate,
        Boolean online,
        String location
) {
}
