package com.training.platform.formation.service;

import com.training.platform.catalogue.formation.entity.Formation;
import com.training.platform.catalogue.formation.repository.FormationRepository;
import com.training.platform.common.exception.ResourceNotFoundException;
import com.training.platform.enrollment.entity.Enrollment;
import com.training.platform.enrollment.entity.EnrollmentStatus;
import com.training.platform.enrollment.repository.EnrollmentRepository;
import com.training.platform.formation.dto.FormationProgressSnapshot;
import com.training.platform.session.entity.SessionStatus;
import com.training.platform.session.repository.TrainingSessionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FormationProgressService {

    private final FormationRepository formationRepository;
    private final TrainingSessionRepository sessionRepository;
    private final EnrollmentRepository enrollmentRepository;

    public FormationProgressService(
            FormationRepository formationRepository,
            TrainingSessionRepository sessionRepository,
            EnrollmentRepository enrollmentRepository
    ) {
        this.formationRepository = formationRepository;
        this.sessionRepository = sessionRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional(readOnly = true)
    public FormationProgressSnapshot snapshot(Long learnerId, Long formationId) {
        int totalSessions = resolveTotalSessions(formationId);
        if (totalSessions == 0) {
            return FormationProgressSnapshot.empty();
        }
        int completedSessions = countCompletedSessions(learnerId, formationId);
        return buildSnapshot(totalSessions, completedSessions);
    }

    @Transactional(readOnly = true)
    public FormationProgressSnapshot snapshot(Long learnerId, Long formationId, List<Enrollment> enrollments) {
        int totalSessions = resolveTotalSessions(formationId);
        if (totalSessions == 0) {
            return FormationProgressSnapshot.empty();
        }
        long completedSessions = enrollments.stream()
                .filter(enrollment -> enrollment.getLearner().getId().equals(learnerId))
                .filter(enrollment -> enrollment.getSession().getFormation().getId().equals(formationId))
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.COMPLETED)
                .filter(enrollment -> enrollment.getSession().getStatus() != SessionStatus.CANCELLED)
                .map(enrollment -> enrollment.getSession().getId())
                .distinct()
                .count();
        return buildSnapshot(totalSessions, (int) completedSessions);
    }

    private int resolveTotalSessions(Long formationId) {
        Formation formation = formationRepository.findById(formationId)
                .orElseThrow(() -> new ResourceNotFoundException("Formation not found with id: " + formationId));
        if (formation.getSessionCount() != null && formation.getSessionCount() > 0) {
            return formation.getSessionCount();
        }
        return (int) sessionRepository.countByFormationIdAndStatusNot(formationId, SessionStatus.CANCELLED);
    }

    private int countCompletedSessions(Long learnerId, Long formationId) {
        return (int) enrollmentRepository.countByLearnerIdAndSessionFormationIdAndStatusAndSessionStatusNot(
                learnerId,
                formationId,
                EnrollmentStatus.COMPLETED,
                SessionStatus.CANCELLED
        );
    }

    private FormationProgressSnapshot buildSnapshot(int totalSessions, int completedSessions) {
        int cappedCompleted = Math.min(completedSessions, totalSessions);
        int progressPercentage = (int) Math.min(100, Math.round(cappedCompleted * 100.0 / totalSessions));
        boolean formationComplete = totalSessions > 0 && cappedCompleted >= totalSessions;
        return new FormationProgressSnapshot(totalSessions, cappedCompleted, progressPercentage, formationComplete);
    }
}
