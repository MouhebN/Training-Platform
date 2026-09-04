package com.training.platform.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.training.platform.catalogue.category.entity.Category;
import com.training.platform.catalogue.formation.entity.Formation;
import com.training.platform.catalogue.formation.entity.FormationLevel;
import com.training.platform.catalogue.formation.service.FormationService;
import com.training.platform.formation.dto.FormationProgressSnapshot;
import com.training.platform.formation.service.FormationProgressService;
import com.training.platform.common.exception.BadRequestException;
import com.training.platform.enrollment.repository.EnrollmentRepository;
import com.training.platform.planning.dto.SessionConflictCheckResponse;
import com.training.platform.planning.service.SessionPlanningService;
import com.training.platform.session.dto.TrainingSessionRequest;
import com.training.platform.session.dto.TrainingSessionResponse;
import com.training.platform.session.entity.SessionStatus;
import com.training.platform.session.entity.TrainingSession;
import com.training.platform.session.repository.TrainingSessionRepository;
import com.training.platform.trainer.entity.TrainerProfile;
import com.training.platform.trainer.service.TrainerService;
import com.training.platform.user.entity.Role;
import com.training.platform.user.entity.User;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrainingSessionServiceTest {

    @Mock
    private TrainingSessionRepository sessionRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private FormationService formationService;

    @Mock
    private TrainerService trainerService;

    @Mock
    private SessionPlanningService planningService;

    @Mock
    private FormationProgressService formationProgressService;

    private TrainingSessionService trainingSessionService;

    @BeforeEach
    void setUp() {
        trainingSessionService = new TrainingSessionService(
                sessionRepository,
                enrollmentRepository,
                formationService,
                trainerService,
                new TrainingSessionMapper(),
                planningService,
                org.mockito.Mockito.mock(com.training.platform.notification.service.NotificationService.class),
                formationProgressService
        );
    }

    @Test
    void createCreatesPlannedSession() {
        Formation formation = formation();
        TrainerProfile trainer = trainer();
        TrainingSession savedSession = session(10L, formation, trainer, SessionStatus.PLANNED);
        TrainingSessionRequest request = new TrainingSessionRequest(
                1L,
                2L,
                "Spring Boot July",
                "Backend session",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(3),
                20,
                null,
                true,
                "https://meet.example.com/session",
                null
        );

        when(formationService.getFormation(1L)).thenReturn(formation);
        when(trainerService.getProfile(2L)).thenReturn(trainer);
        when(sessionRepository.countByFormationIdAndStatusNot(1L, SessionStatus.CANCELLED)).thenReturn(0L);
        when(planningService.checkConflicts(any(), any())).thenReturn(new SessionConflictCheckResponse(false, false, List.of()));
        when(planningService.hasBlockingConflicts(any())).thenReturn(false);
        when(sessionRepository.save(any(TrainingSession.class))).thenReturn(savedSession);
        when(enrollmentRepository.findBySessionIdOrderByEnrolledAtDesc(any())).thenReturn(List.of());
        when(enrollmentRepository.countBySessionIdAndStatusIn(any(), any())).thenReturn(0L);

        TrainingSessionResponse response = trainingSessionService.create(request);

        ArgumentCaptor<TrainingSession> captor = ArgumentCaptor.forClass(TrainingSession.class);
        verify(sessionRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues().getFirst().getStatus()).isEqualTo(SessionStatus.PLANNED);
        assertThat(captor.getAllValues().getFirst().getOnline()).isTrue();
        assertThat(response.availablePlaces()).isEqualTo(20);
    }

    @Test
    void createRejectsWhenFormationSessionLimitReached() {
        Formation formation = formation();
        formation.setSessionCount(2);
        TrainingSessionRequest request = new TrainingSessionRequest(
                1L,
                2L,
                "Extra session",
                null,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                10,
                null,
                true,
                null,
                null
        );

        when(formationService.getFormation(1L)).thenReturn(formation);
        when(sessionRepository.countByFormationIdAndStatusNot(1L, SessionStatus.CANCELLED)).thenReturn(2L);

        assertThatThrownBy(() -> trainingSessionService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("This formation allows 2 session(s). Cancel or delete an existing session before adding another.");
    }

    @Test
    void createRejectsInvalidDateRange() {
        LocalDateTime start = LocalDateTime.now().plusDays(3);
        TrainingSessionRequest request = new TrainingSessionRequest(
                1L,
                2L,
                "Invalid",
                null,
                start,
                start.minusHours(1),
                10,
                null,
                true,
                null,
                null
        );

        assertThatThrownBy(() -> trainingSessionService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Session start date must be before end date");
    }

    @Test
    void completeMarksPresentLearnersAndGrantsFormationSkills() {
        Formation formation = formation();
        com.training.platform.skill.entity.Skill spring = com.training.platform.skill.entity.Skill.builder()
                .id(9L)
                .name("Spring Boot")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        if (formation.getRequiredSkills() == null) {
            formation.setRequiredSkills(new java.util.HashSet<>());
        }
        formation.getRequiredSkills().add(spring);
        TrainerProfile trainer = trainer();
        TrainingSession session = session(10L, formation, trainer, SessionStatus.OPEN);
        com.training.platform.learner.entity.LearnerProfile learner = com.training.platform.learner.entity.LearnerProfile.builder()
                .id(4L)
                .user(user(4L, "learner@test.com", Role.LEARNER))
                .skills(new java.util.HashSet<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        com.training.platform.enrollment.entity.Enrollment enrollment = com.training.platform.enrollment.entity.Enrollment.builder()
                .id(21L)
                .learner(learner)
                .session(session)
                .status(com.training.platform.enrollment.entity.EnrollmentStatus.CONFIRMED)
                .enrolledAt(LocalDateTime.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(sessionRepository.findById(10L)).thenReturn(java.util.Optional.of(session));
        when(enrollmentRepository.findBySessionIdOrderByEnrolledAtDesc(10L)).thenReturn(List.of(enrollment));
        when(sessionRepository.save(any(TrainingSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(enrollmentRepository.countBySessionIdAndStatusIn(any(), any())).thenReturn(1L);
        when(formationProgressService.snapshot(4L, 1L)).thenReturn(new FormationProgressSnapshot(1, 1, 100, true));

        TrainingSessionResponse response = trainingSessionService.complete(10L, List.of(21L));

        assertThat(response.status()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(enrollment.getStatus()).isEqualTo(com.training.platform.enrollment.entity.EnrollmentStatus.COMPLETED);
        assertThat(learner.getSkills()).contains(spring);
    }

    @Test
    void startMovesSessionToInProgress() {
        Formation formation = formation();
        TrainerProfile trainer = trainer();
        TrainingSession session = session(10L, formation, trainer, SessionStatus.OPEN);

        when(sessionRepository.findById(10L)).thenReturn(java.util.Optional.of(session));
        when(enrollmentRepository.findBySessionIdOrderByEnrolledAtDesc(10L)).thenReturn(List.of());
        when(sessionRepository.save(any(TrainingSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(enrollmentRepository.countBySessionIdAndStatusIn(any(), any())).thenReturn(0L);

        TrainingSessionResponse response = trainingSessionService.start(10L);

        assertThat(response.status()).isEqualTo(SessionStatus.IN_PROGRESS);
        assertThat(session.getStatus()).isEqualTo(SessionStatus.IN_PROGRESS);
    }

    @Test
    void cancelMarksSessionCancelled() {
        Formation formation = formation();
        TrainerProfile trainer = trainer();
        TrainingSession session = session(10L, formation, trainer, SessionStatus.OPEN);

        when(sessionRepository.findById(10L)).thenReturn(java.util.Optional.of(session));
        when(enrollmentRepository.findBySessionIdOrderByEnrolledAtDesc(10L)).thenReturn(List.of());
        when(sessionRepository.save(any(TrainingSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(enrollmentRepository.countBySessionIdAndStatusIn(any(), any())).thenReturn(0L);

        TrainingSessionResponse response = trainingSessionService.cancel(10L);

        assertThat(response.status()).isEqualTo(SessionStatus.CANCELLED);
    }

    @Test
    void remindStoresReminderTimestamp() {
        Formation formation = formation();
        TrainerProfile trainer = trainer();
        TrainingSession session = session(10L, formation, trainer, SessionStatus.OPEN);

        when(sessionRepository.findById(10L)).thenReturn(java.util.Optional.of(session));
        when(enrollmentRepository.findBySessionIdOrderByEnrolledAtDesc(10L)).thenReturn(List.of());
        when(sessionRepository.save(any(TrainingSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(enrollmentRepository.countBySessionIdAndStatusIn(any(), any())).thenReturn(0L);

        trainingSessionService.remind(10L);

        assertThat(session.getReminderSentAt()).isNotNull();
    }

    private TrainingSession session(Long id, Formation formation, TrainerProfile trainer, SessionStatus status) {
        return TrainingSession.builder()
                .id(id)
                .formation(formation)
                .trainer(trainer)
                .title("Spring Boot July")
                .description("Backend session")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(3))
                .capacity(20)
                .online(true)
                .meetingUrl("https://meet.example.com/session")
                .status(status)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private Formation formation() {
        return Formation.builder()
                .id(1L)
                .title("Spring Boot Fundamentals")
                .description("Backend")
                .price(BigDecimal.valueOf(250))
                .level(FormationLevel.BEGINNER)
                .durationHours(24)
                .sessionCount(2)
                .active(true)
                .category(Category.builder().id(1L).name("IT").createdAt(Instant.now()).updatedAt(Instant.now()).build())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private TrainerProfile trainer() {
        return TrainerProfile.builder()
                .id(2L)
                .user(user(3L, "trainer@test.com", Role.TRAINER))
                .yearsOfExperience(4)
                .expertise(new java.util.LinkedHashSet<>(List.of()))
                .averageRating(0.0)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private User user(Long id, String email, Role role) {
        return User.builder()
                .id(id)
                .firstName("Trainer")
                .lastName("One")
                .email(email)
                .password("encoded")
                .role(role)
                .enabled(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
