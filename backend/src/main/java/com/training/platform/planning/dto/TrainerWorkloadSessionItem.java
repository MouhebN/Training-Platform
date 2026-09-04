package com.training.platform.planning.dto;

import com.training.platform.session.entity.SessionStatus;
import java.time.LocalDateTime;

public record TrainerWorkloadSessionItem(
        Long sessionId,
        String sessionTitle,
        String formationTitle,
        LocalDateTime startDate,
        LocalDateTime endDate,
        SessionStatus status,
        long durationHours
) {
}
