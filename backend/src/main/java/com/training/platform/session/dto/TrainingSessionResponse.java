package com.training.platform.session.dto;

import com.training.platform.session.entity.SessionStatus;
import java.time.LocalDateTime;

public record TrainingSessionResponse(
        Long id,
        Long formationId,
        String formationTitle,
        Integer formationSessionCount,
        Long trainerId,
        String trainerFullName,
        String title,
        String description,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Integer capacity,
        long enrolledCount,
        long availablePlaces,
        String location,
        Boolean online,
        String meetingUrl,
        SessionStatus status
) {
}
