package com.training.platform.formation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.training.platform.catalogue.formation.entity.Formation;
import com.training.platform.catalogue.formation.repository.FormationRepository;
import com.training.platform.enrollment.entity.Enrollment;
import com.training.platform.enrollment.entity.EnrollmentStatus;
import com.training.platform.enrollment.repository.EnrollmentRepository;
import com.training.platform.formation.dto.FormationProgressSnapshot;
import com.training.platform.session.entity.SessionStatus;
import com.training.platform.session.entity.TrainingSession;
import com.training.platform.session.repository.TrainingSessionRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FormationProgressServiceTest {

    @Mock
    private FormationRepository formationRepository;

    @Mock
    private TrainingSessionRepository sessionRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    private FormationProgressService service;

    @BeforeEach
    void setUp() {
        service = new FormationProgressService(formationRepository, sessionRepository, enrollmentRepository);
    }

    @Test
    void oneOfFourPlannedSessionsIsTwentyFivePercent() {
        when(formationRepository.findById(1L)).thenReturn(Optional.of(formation(4)));
        when(enrollmentRepository.countByLearnerIdAndSessionFormationIdAndStatusAndSessionStatusNot(
                5L, 1L, EnrollmentStatus.COMPLETED, SessionStatus.CANCELLED
        )).thenReturn(1L);

        FormationProgressSnapshot snapshot = service.snapshot(5L, 1L);

        assertThat(snapshot.totalSessions()).isEqualTo(4);
        assertThat(snapshot.completedSessions()).isEqualTo(1);
        assertThat(snapshot.progressPercentage()).isEqualTo(25);
        assertThat(snapshot.formationComplete()).isFalse();
    }

    @Test
    void allPlannedSessionsCompletedMarksFormationComplete() {
        when(formationRepository.findById(1L)).thenReturn(Optional.of(formation(4)));
        when(enrollmentRepository.countByLearnerIdAndSessionFormationIdAndStatusAndSessionStatusNot(
                5L, 1L, EnrollmentStatus.COMPLETED, SessionStatus.CANCELLED
        )).thenReturn(4L);

        FormationProgressSnapshot snapshot = service.snapshot(5L, 1L);

        assertThat(snapshot.progressPercentage()).isEqualTo(100);
        assertThat(snapshot.formationComplete()).isTrue();
    }

    @Test
    void snapshotFromEnrollmentsUsesFormationSessionCount() {
        TrainingSession completedSession = TrainingSession.builder()
                .id(10L)
                .formation(formation(2))
                .title("Session 1")
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusHours(2))
                .capacity(10)
                .online(true)
                .status(SessionStatus.COMPLETED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        Enrollment enrollment = Enrollment.builder()
                .id(99L)
                .learner(com.training.platform.learner.entity.LearnerProfile.builder().id(5L).build())
                .session(completedSession)
                .status(EnrollmentStatus.COMPLETED)
                .enrolledAt(LocalDateTime.now())
                .build();

        when(formationRepository.findById(1L)).thenReturn(Optional.of(formation(2)));

        FormationProgressSnapshot snapshot = service.snapshot(5L, 1L, List.of(enrollment));

        assertThat(snapshot.completedSessions()).isEqualTo(1);
        assertThat(snapshot.progressPercentage()).isEqualTo(50);
    }

    private Formation formation(int sessionCount) {
        return Formation.builder()
                .id(1L)
                .title("Spring Boot")
                .sessionCount(sessionCount)
                .durationHours(20)
                .active(true)
                .build();
    }
}
