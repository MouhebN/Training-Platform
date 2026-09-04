package com.training.platform.enrollment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.training.platform.common.service.EmailService;
import com.training.platform.catalogue.category.entity.Category;
import com.training.platform.catalogue.formation.entity.Formation;
import com.training.platform.catalogue.formation.entity.FormationLevel;
import com.training.platform.common.exception.BadRequestException;
import com.training.platform.formation.service.FormationProgressService;
import com.training.platform.enrollment.dto.EnrollmentResponse;
import com.training.platform.enrollment.entity.Enrollment;
import com.training.platform.enrollment.entity.EnrollmentStatus;
import com.training.platform.enrollment.repository.EnrollmentRepository;
import com.training.platform.learner.entity.LearnerLevel;
import com.training.platform.learner.entity.LearnerProfile;
import com.training.platform.learner.repository.LearnerProfileRepository;
import com.training.platform.session.entity.SessionStatus;
import com.training.platform.session.entity.TrainingSession;
import com.training.platform.session.service.TrainingSessionService;
import com.training.platform.trainer.entity.TrainerProfile;
import com.training.platform.user.entity.Role;
import com.training.platform.user.entity.User;
import java.math.BigDecimal;
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
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private LearnerProfileRepository learnerProfileRepository;

    @Mock
    private TrainingSessionService trainingSessionService;

    @Mock
    private EmailService emailService;

    private EnrollmentService enrollmentService;

    @BeforeEach
    void setUp() {
        enrollmentService = new EnrollmentService(
                enrollmentRepository,
                learnerProfileRepository,
                trainingSessionService,
                new EnrollmentMapper(),
                emailService,
                org.mockito.Mockito.mock(com.training.platform.notification.service.NotificationService.class),
                org.mockito.Mockito.mock(FormationProgressService.class)
        );
    }

    @Test
    void enrollCreatesConfirmedEnrollment() {
        LearnerProfile learner = learner();
        TrainingSession session = session(SessionStatus.OPEN, 10);
        Enrollment saved = enrollment(1L, learner, session);

        when(learnerProfileRepository.findByUserEmail("learner@test.com")).thenReturn(Optional.of(learner));
        when(trainingSessionService.getSession(5L)).thenReturn(session);
        when(enrollmentRepository.findByLearnerIdAndSessionId(learner.getId(), session.getId())).thenReturn(Optional.empty());
        when(enrollmentRepository.countBySessionIdAndStatusIn(any(), any())).thenReturn(0L);
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(saved);

        EnrollmentResponse response = enrollmentService.enroll(5L, "learner@test.com");

        assertThat(response.status()).isEqualTo(EnrollmentStatus.CONFIRMED);
        assertThat(response.learnerFullName()).isEqualTo("Learner One");
        assertThat(response.sessionId()).isEqualTo(5L);
    }

    @Test
    void enrollRejectsDuplicateEnrollment() {
        LearnerProfile learner = learner();
        TrainingSession session = session(SessionStatus.OPEN, 10);

        when(learnerProfileRepository.findByUserEmail("learner@test.com")).thenReturn(Optional.of(learner));
        when(trainingSessionService.getSession(5L)).thenReturn(session);
        when(enrollmentRepository.findByLearnerIdAndSessionId(learner.getId(), session.getId()))
                .thenReturn(Optional.of(enrollment(1L, learner, session)));

        assertThatThrownBy(() -> enrollmentService.enroll(5L, "learner@test.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Learner is already enrolled in this session");
    }

    @Test
    void enrollCreatesWaitlistedEnrollmentWhenCapacityReached() {
        LearnerProfile learner = learner();
        TrainingSession session = session(SessionStatus.OPEN, 1);

        when(learnerProfileRepository.findByUserEmail("learner@test.com")).thenReturn(Optional.of(learner));
        when(trainingSessionService.getSession(5L)).thenReturn(session);
        when(enrollmentRepository.findByLearnerIdAndSessionId(learner.getId(), session.getId())).thenReturn(Optional.empty());
        when(enrollmentRepository.countBySessionIdAndStatusIn(any(), any())).thenReturn(1L);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> {
            Enrollment enrollment = invocation.getArgument(0);
            enrollment.setId(20L);
            return enrollment;
        });

        EnrollmentResponse response = enrollmentService.enroll(5L, "learner@test.com");

        assertThat(response.status()).isEqualTo(EnrollmentStatus.WAITLISTED);
    }

    @Test
    void enrollReopensCancelledEnrollment() {
        LearnerProfile learner = learner();
        TrainingSession session = session(SessionStatus.OPEN, 10);
        Enrollment cancelled = enrollment(1L, learner, session);
        cancelled.setStatus(EnrollmentStatus.CANCELLED);

        when(learnerProfileRepository.findByUserEmail("learner@test.com")).thenReturn(Optional.of(learner));
        when(trainingSessionService.getSession(5L)).thenReturn(session);
        when(enrollmentRepository.findByLearnerIdAndSessionId(learner.getId(), session.getId())).thenReturn(Optional.of(cancelled));
        when(enrollmentRepository.countBySessionIdAndStatusIn(any(), any())).thenReturn(0L);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EnrollmentResponse response = enrollmentService.enroll(5L, "learner@test.com");

        assertThat(response.status()).isEqualTo(EnrollmentStatus.CONFIRMED);
        assertThat(cancelled.getStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
    }

    @Test
    void cancelConfirmedEnrollmentPromotesOldestWaitlistedLearner() {
        LearnerProfile learner = learner();
        LearnerProfile waitlistedLearner = LearnerProfile.builder()
                .id(7L)
                .user(user(7L, "wait@test.com", Role.LEARNER, "Wait", "Listed"))
                .currentLevel(LearnerLevel.BEGINNER)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        TrainingSession session = session(SessionStatus.OPEN, 1);
        Enrollment confirmed = enrollment(1L, learner, session);
        Enrollment waitlisted = Enrollment.builder()
                .id(2L)
                .learner(waitlistedLearner)
                .session(session)
                .status(EnrollmentStatus.WAITLISTED)
                .enrolledAt(LocalDateTime.now().minusHours(1))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(confirmed));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(enrollmentRepository.findFirstBySessionIdAndStatusOrderByEnrolledAtAsc(5L, EnrollmentStatus.WAITLISTED))
                .thenReturn(Optional.of(waitlisted));

        var response = enrollmentService.cancel(1L);

        assertThat(response.promoted()).isTrue();
        assertThat(waitlisted.getStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
        verify(emailService).sendWaitlistPromotion("wait@test.com", "Wait Listed", "Spring Boot July");
    }

    private Enrollment enrollment(Long id, LearnerProfile learner, TrainingSession session) {
        return Enrollment.builder()
                .id(id)
                .learner(learner)
                .session(session)
                .status(EnrollmentStatus.CONFIRMED)
                .enrolledAt(LocalDateTime.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private LearnerProfile learner() {
        return LearnerProfile.builder()
                .id(4L)
                .user(user(4L, "learner@test.com", Role.LEARNER, "Learner", "One"))
                .currentLevel(LearnerLevel.BEGINNER)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private TrainingSession session(SessionStatus status, int capacity) {
        return TrainingSession.builder()
                .id(5L)
                .formation(formation())
                .trainer(trainer())
                .title("Spring Boot July")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(3))
                .capacity(capacity)
                .online(true)
                .status(status)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private Formation formation() {
        return Formation.builder()
                .id(1L)
                .title("Spring Boot Fundamentals")
                .price(BigDecimal.valueOf(250))
                .level(FormationLevel.BEGINNER)
                .durationHours(24)
                .active(true)
                .category(Category.builder().id(1L).name("IT").createdAt(Instant.now()).updatedAt(Instant.now()).build())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private TrainerProfile trainer() {
        return TrainerProfile.builder()
                .id(2L)
                .user(user(2L, "trainer@test.com", Role.TRAINER, "Trainer", "One"))
                .yearsOfExperience(4)
                .expertise(new java.util.LinkedHashSet<>(List.of()))
                .averageRating(0.0)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private User user(Long id, String email, Role role, String firstName, String lastName) {
        return User.builder()
                .id(id)
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .password("encoded")
                .role(role)
                .enabled(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
