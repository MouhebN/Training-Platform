package com.training.platform.session.service;

import com.training.platform.session.dto.TrainingSessionResponse;
import com.training.platform.session.entity.TrainingSession;
import org.springframework.stereotype.Component;

@Component
public class TrainingSessionMapper {

    public TrainingSessionResponse toResponse(TrainingSession session, long enrolledCount) {
        String trainerFullName = session.getTrainer().getUser().getFirstName()
                + " "
                + session.getTrainer().getUser().getLastName();
        long availablePlaces = Math.max(0, session.getCapacity() - enrolledCount);

        return new TrainingSessionResponse(
                session.getId(),
                session.getFormation().getId(),
                session.getFormation().getTitle(),
                session.getFormation().getSessionCount(),
                session.getTrainer().getId(),
                trainerFullName,
                session.getTitle(),
                session.getDescription(),
                session.getStartDate(),
                session.getEndDate(),
                session.getCapacity(),
                enrolledCount,
                availablePlaces,
                session.getLocation(),
                session.getOnline(),
                session.getMeetingUrl(),
                session.getStatus()
        );
    }
}
