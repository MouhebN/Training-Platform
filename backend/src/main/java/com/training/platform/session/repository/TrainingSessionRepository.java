package com.training.platform.session.repository;

import com.training.platform.session.entity.TrainingSession;
import com.training.platform.session.entity.SessionStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TrainingSessionRepository extends JpaRepository<TrainingSession, Long>, JpaSpecificationExecutor<TrainingSession> {

    List<TrainingSession> findByFormationId(Long formationId);

    long countByFormationIdAndStatusNot(Long formationId, SessionStatus status);

    List<TrainingSession> findByTrainerUserEmail(String email);

    boolean existsByFormationIdAndStatusIn(Long formationId, Collection<SessionStatus> statuses);

    List<TrainingSession> findByTrainerIdAndStatusIn(Long trainerId, Collection<SessionStatus> statuses);

    List<TrainingSession> findByTrainerIdAndStartDateLessThanAndEndDateGreaterThanAndStatusIn(
            Long trainerId,
            LocalDateTime endDate,
            LocalDateTime startDate,
            Collection<SessionStatus> statuses
    );

    List<TrainingSession> findByOnlineFalseAndLocationIgnoreCaseAndStartDateLessThanAndEndDateGreaterThanAndStatusIn(
            String location,
            LocalDateTime endDate,
            LocalDateTime startDate,
            Collection<SessionStatus> statuses
    );

    List<TrainingSession> findByTrainerIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long trainerId,
            LocalDateTime endDate,
            LocalDateTime startDate
    );

    List<TrainingSession> findByTrainerId(Long trainerId);

    List<TrainingSession> findByStatusInAndStartDateBetweenAndReminderSentAtIsNull(
            Collection<SessionStatus> statuses,
            LocalDateTime from,
            LocalDateTime to
    );

    List<TrainingSession> findByStatusInAndStartDateBetweenAndHourReminderSentAtIsNull(
            Collection<SessionStatus> statuses,
            LocalDateTime from,
            LocalDateTime to
    );

    void deleteByTrainerId(Long trainerId);
}
