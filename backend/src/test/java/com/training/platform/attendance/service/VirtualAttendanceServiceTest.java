package com.training.platform.attendance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.training.platform.attendance.config.AttendanceProperties;
import com.training.platform.attendance.dto.ClassroomAttendanceReportResponse;
import com.training.platform.attendance.dto.ClassroomContextResponse;
import com.training.platform.attendance.entity.ClassroomPresenceInterval;
import com.training.platform.attendance.repository.ClassroomPresenceIntervalRepository;
import com.training.platform.catalogue.category.entity.Category;
import com.training.platform.catalogue.formation.entity.Formation;
import com.training.platform.catalogue.formation.entity.FormationLevel;
import com.training.platform.common.exception.BadRequestException;
import com.training.platform.enrollment.entity.Enrollment;
import com.training.platform.enrollment.entity.EnrollmentStatus;
import com.training.platform.enrollment.repository.EnrollmentRepository;
import com.training.platform.learner.entity.LearnerLevel;
import com.training.platform.learner.entity.LearnerProfile;
import com.training.platform.session.entity.SessionStatus;
import com.training.platform.session.entity.TrainingSession;
import com.training.platform.session.repository.TrainingSessionRepository;
import com.training.platform.session.service.TrainingSessionService;
import com.training.platform.trainer.entity.TrainerProfile;
import com.training.platform.user.entity.Role;
import com.training.platform.user.entity.User;
import com.training.platform.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class VirtualAttendanceServiceTest {

    @Mock
    private ClassroomPresenceIntervalRepository presenceRepository;

    @Mock
    private TrainingSessionService sessionService;

    @Mock
    private TrainingSessionRepository sessionRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private UserRepository userRepository;

    private VirtualAttendanceService service;

    private User trainerUser;
    private User learnerUser;
    private TrainingSession session;
    private Enrollment enrollment;

    @BeforeEach
    void setUp() {
        service = new VirtualAttendanceService(
                presenceRepository,
                sessionService,
                sessionRepository,
                enrollmentRepository,
                userRepository,
                new AttendanceProperties(70, 30, 90, "meet.jit.si")
        );
        trainerUser = user(1L, "trainer@test.com", Role.TRAINER, "Tina", "Trainer");
        learnerUser = user(2L, "learner@test.com", Role.LEARNER, "Lea", "Learner");
        session = onlineSession();
        enrollment = enrollment(learnerUser, session, EnrollmentStatus.CONFIRMED);
    }

    @Test
    void confirmedLearnerCanJoinClassroom() {
        when(userRepository.findByEmail("learner@test.com")).thenReturn(Optional.of(learnerUser));
        when(sessionService.getSession(5L)).thenReturn(session);
        when(presenceRepository.findBySessionIdOrderByJoinedAtAsc(5L)).thenReturn(List.of());
        when(presenceRepository.findFirstBySessionIdAndUserIdAndLeftAtIsNull(5L, 2L)).thenReturn(Optional.empty());
        when(enrollmentRepository.findBySessionIdOrderByEnrolledAtDesc(5L)).thenReturn(List.of(enrollment));
        when(presenceRepository.save(any(ClassroomPresenceInterval.class))).thenAnswer(invocation -> {
            ClassroomPresenceInterval interval = invocation.getArgument(0);
            interval.setId(99L);
            return interval;
        });

        ClassroomContextResponse response = service.join(5L, "learner@test.com");

        assertThat(response.roomName()).isEqualTo("training-platform-room-key-123");
        assertThat(response.moderator()).isFalse();
        ArgumentCaptor<ClassroomPresenceInterval> captor = ArgumentCaptor.forClass(ClassroomPresenceInterval.class);
        verify(presenceRepository).save(captor.capture());
        assertThat(captor.getValue().getEnrollment()).isEqualTo(enrollment);
    }

    @Test
    void waitlistedLearnerCannotAccessClassroom() {
        Enrollment waitlisted = enrollment(learnerUser, session, EnrollmentStatus.WAITLISTED);
        when(userRepository.findByEmail("learner@test.com")).thenReturn(Optional.of(learnerUser));
        when(sessionService.getSession(5L)).thenReturn(session);
        when(enrollmentRepository.findBySessionIdOrderByEnrolledAtDesc(5L)).thenReturn(List.of(waitlisted));

        assertThatThrownBy(() -> service.getContext(5L, "learner@test.com"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void attendanceReportUsesTrainerIntersection() {
        Instant now = Instant.now();
        Instant start = now.minusSeconds(3600);
        List<ClassroomPresenceInterval> intervals = new ArrayList<>();
        intervals.add(presence(1L, trainerUser, start, now, null));
        intervals.add(presence(2L, learnerUser, start.plusSeconds(600), now, null));

        when(userRepository.findByEmail("trainer@test.com")).thenReturn(Optional.of(trainerUser));
        when(sessionService.getSession(5L)).thenReturn(session);
        when(presenceRepository.findBySessionIdOrderByJoinedAtAsc(5L)).thenReturn(intervals);
        when(enrollmentRepository.findBySessionIdOrderByEnrolledAtDesc(5L)).thenReturn(List.of(enrollment));

        ClassroomAttendanceReportResponse report = service.getAttendanceReport(5L, "trainer@test.com");

        assertThat(report.trainerActiveSeconds()).isEqualTo(3600);
        assertThat(report.learners()).hasSize(1);
        assertThat(report.learners().getFirst().trackedSeconds()).isEqualTo(3000);
        assertThat(report.learners().getFirst().attendancePercentage()).isEqualTo(83);
        assertThat(report.learners().getFirst().qualified()).isTrue();
    }

    @Test
    void completeSmartRequiresTrainerPresence() {
        when(userRepository.findByEmail("trainer@test.com")).thenReturn(Optional.of(trainerUser));
        when(sessionService.getSession(5L)).thenReturn(session);
        when(presenceRepository.findBySessionIdOrderByJoinedAtAsc(5L)).thenReturn(List.of());
        when(enrollmentRepository.findBySessionIdOrderByEnrolledAtDesc(5L)).thenReturn(List.of(enrollment));

        assertThatThrownBy(() -> service.completeSmart(5L, "trainer@test.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("trainer was not present");
    }

    private User user(Long id, String email, Role role, String firstName, String lastName) {
        return User.builder()
                .id(id)
                .email(email)
                .role(role)
                .firstName(firstName)
                .lastName(lastName)
                .password("secret")
                .build();
    }

    private TrainingSession onlineSession() {
        TrainerProfile trainer = TrainerProfile.builder()
                .id(10L)
                .user(trainerUser)
                .bio("bio")
                .yearsOfExperience(5)
                .build();
        Formation formation = Formation.builder()
                .id(3L)
                .title("Spring Boot")
                .description("desc")
                .level(FormationLevel.INTERMEDIATE)
                .durationHours(20)
                .price(BigDecimal.TEN)
                .active(true)
                .category(Category.builder().id(1L).name("Dev").createdAt(Instant.now()).updatedAt(Instant.now()).build())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        return TrainingSession.builder()
                .id(5L)
                .title("Live workshop")
                .formation(formation)
                .trainer(trainer)
                .startDate(LocalDateTime.now().minusHours(1))
                .endDate(LocalDateTime.now().plusHours(2))
                .capacity(20)
                .online(true)
                .classroomRoomKey("room-key-123")
                .status(SessionStatus.IN_PROGRESS)
                .build();
    }

    private Enrollment enrollment(User learnerUser, TrainingSession session, EnrollmentStatus status) {
        LearnerProfile learner = LearnerProfile.builder()
                .id(20L)
                .user(learnerUser)
                .currentLevel(LearnerLevel.BEGINNER)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        return Enrollment.builder()
                .id(30L)
                .learner(learner)
                .session(session)
                .status(status)
                .enrolledAt(LocalDateTime.now())
                .build();
    }

    private ClassroomPresenceInterval presence(
            Long id,
            User user,
            Instant joinedAt,
            Instant lastHeartbeatAt,
            Instant leftAt
    ) {
        return ClassroomPresenceInterval.builder()
                .id(id)
                .session(session)
                .user(user)
                .joinedAt(joinedAt)
                .lastHeartbeatAt(lastHeartbeatAt)
                .leftAt(leftAt)
                .build();
    }
}
