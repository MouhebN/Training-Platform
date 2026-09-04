package com.training.platform.learningpath.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.training.platform.catalogue.category.entity.Category;
import com.training.platform.catalogue.formation.entity.Formation;
import com.training.platform.catalogue.formation.entity.FormationLevel;
import com.training.platform.catalogue.formation.repository.FormationRepository;
import com.training.platform.enrollment.entity.Enrollment;
import com.training.platform.enrollment.entity.EnrollmentStatus;
import com.training.platform.enrollment.repository.EnrollmentRepository;
import com.training.platform.learner.entity.LearnerLevel;
import com.training.platform.learner.entity.LearnerProfile;
import com.training.platform.learner.repository.LearnerProfileRepository;
import com.training.platform.learningpath.dto.LearningPathResponse;
import com.training.platform.learningpath.dto.LearningPathStepStatus;
import com.training.platform.formation.dto.FormationProgressSnapshot;
import com.training.platform.formation.service.FormationProgressService;
import com.training.platform.session.entity.SessionStatus;
import com.training.platform.session.entity.TrainingSession;
import com.training.platform.session.repository.TrainingSessionRepository;
import com.training.platform.skill.entity.Skill;
import com.training.platform.user.entity.Role;
import com.training.platform.user.entity.User;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LearningPathServiceTest {

    @Mock
    private LearnerProfileRepository learnerProfileRepository;

    @Mock
    private FormationRepository formationRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private TrainingSessionRepository sessionRepository;

    @Mock
    private FormationProgressService formationProgressService;

    private LearningPathService learningPathService;

    @BeforeEach
    void setUp() {
        learningPathService = new LearningPathService(
                learnerProfileRepository,
                formationRepository,
                enrollmentRepository,
                sessionRepository,
                formationProgressService
        );
    }

    @Test
    void generateCalculatesGlobalProgressAndCompletedStatus() {
        LearnerProfile learner = learner("spring backend", LearnerLevel.BEGINNER, Set.of(skill(1L, "Java")));
        Formation completed = formation(10L, "Java Basics", FormationLevel.BEGINNER, Set.of(skill(1L, "Java")));
        Formation next = formation(11L, "Spring Boot Fundamentals", FormationLevel.BEGINNER, Set.of(skill(1L, "Java"), skill(2L, "Spring Boot")));

        stubBase(learner, List.of(completed, next), List.of(enrollment(completed, EnrollmentStatus.COMPLETED)));

        LearningPathResponse response = learningPathService.generate("learner@test.com");

        assertThat(response.completedSteps()).isEqualTo(1);
        assertThat(response.totalSteps()).isEqualTo(2);
        assertThat(response.globalProgressPercentage()).isEqualTo(50);
        assertThat(response.steps().getFirst().status()).isEqualTo(LearningPathStepStatus.COMPLETED);
    }

    @Test
    void generateMarksConfirmedEnrollmentAsInProgress() {
        LearnerProfile learner = learner("angular frontend", LearnerLevel.BEGINNER, Set.of(skill(3L, "Angular")));
        Formation angular = formation(20L, "Angular Essentials", FormationLevel.BEGINNER, Set.of(skill(3L, "Angular")));
        Formation typescript = formation(21L, "TypeScript Basics", FormationLevel.BEGINNER, Set.of(skill(4L, "TypeScript")));

        stubBase(learner, List.of(angular, typescript), List.of(enrollment(angular, EnrollmentStatus.CONFIRMED)));

        LearningPathResponse response = learningPathService.generate("learner@test.com");

        assertThat(response.steps()).anySatisfy(step -> {
            assertThat(step.formationTitle()).isEqualTo("Angular Essentials");
            assertThat(step.status()).isEqualTo(LearningPathStepStatus.IN_PROGRESS);
        });
    }

    @Test
    void generateSelectsRecommendedNextFromGoalAndSkills() {
        LearnerProfile learner = learner("spring backend", LearnerLevel.BEGINNER, Set.of(skill(1L, "Java")));
        Formation angular = formation(30L, "Angular Essentials", FormationLevel.BEGINNER, Set.of(skill(3L, "Angular")));
        Formation spring = formation(31L, "Spring Boot Fundamentals", FormationLevel.BEGINNER, Set.of(skill(1L, "Java"), skill(2L, "Spring Boot")));

        stubBase(learner, List.of(angular, spring), List.of());

        LearningPathResponse response = learningPathService.generate("learner@test.com");

        assertThat(response.nextRecommendedFormationTitle()).isEqualTo("Spring Boot Fundamentals");
        assertThat(response.steps()).anySatisfy(step -> {
            assertThat(step.formationTitle()).isEqualTo("Spring Boot Fundamentals");
            assertThat(step.status()).isEqualTo(LearningPathStepStatus.RECOMMENDED_NEXT);
        });
    }

    @Test
    void generateLocksAdvancedFormationWhenMatchIsLow() {
        LearnerProfile learner = learner("spring backend", LearnerLevel.BEGINNER, Set.of(skill(1L, "Java")));
        Formation next = formation(40L, "Java Basics", FormationLevel.BEGINNER, Set.of(skill(1L, "Java")));
        Formation advanced = formation(41L, "Advanced Security Architecture", FormationLevel.ADVANCED, Set.of(skill(5L, "Security"), skill(6L, "Architecture")));

        stubBase(learner, List.of(advanced, next), List.of());

        LearningPathResponse response = learningPathService.generate("learner@test.com");

        assertThat(response.steps()).anySatisfy(step -> {
            assertThat(step.formationTitle()).isEqualTo("Advanced Security Architecture");
            assertThat(step.status()).isEqualTo(LearningPathStepStatus.LOCKED);
            assertThat(step.reason()).isEqualTo("Locked until you improve missing skills");
        });
    }

    @Test
    void generateIncludesEstimatedTotalHoursAndAvailableSessions() {
        LearnerProfile learner = learner("project management", LearnerLevel.INTERMEDIATE, Set.of(skill(7L, "Project Management")));
        Formation management = formation(50L, "Project Management Professional", FormationLevel.INTERMEDIATE, Set.of(skill(7L, "Project Management")));
        Formation communication = formation(51L, "Business Communication", FormationLevel.INTERMEDIATE, Set.of(skill(8L, "Communication")));

        stubBase(learner, List.of(management, communication), List.of());
        when(sessionRepository.existsByFormationIdAndStatusIn(50L, List.of(SessionStatus.OPEN, SessionStatus.PLANNED))).thenReturn(true);

        LearningPathResponse response = learningPathService.generate("learner@test.com");

        assertThat(response.estimatedTotalHours()).isEqualTo(48);
        assertThat(response.steps()).anySatisfy(step -> {
            assertThat(step.formationTitle()).isEqualTo("Project Management Professional");
            assertThat(step.hasAvailableSession()).isTrue();
        });
    }

    private void stubBase(LearnerProfile learner, List<Formation> formations, List<Enrollment> enrollments) {
        when(learnerProfileRepository.findByUserEmail("learner@test.com")).thenReturn(Optional.of(learner));
        when(formationRepository.findByActiveTrue()).thenReturn(formations);
        when(enrollmentRepository.findByLearnerUserEmailOrderByEnrolledAtDesc("learner@test.com")).thenReturn(enrollments);
        when(sessionRepository.existsByFormationIdAndStatusIn(anyLong(), any())).thenReturn(false);
        when(formationProgressService.snapshot(org.mockito.ArgumentMatchers.eq(5L), anyLong(), any()))
                .thenAnswer(invocation -> {
                    Long formationId = invocation.getArgument(1);
                    boolean completed = enrollments.stream()
                            .anyMatch(enrollment ->
                                    enrollment.getSession().getFormation().getId().equals(formationId)
                                            && enrollment.getStatus() == EnrollmentStatus.COMPLETED
                            );
                    return completed
                            ? new FormationProgressSnapshot(1, 1, 100, true)
                            : new FormationProgressSnapshot(1, 0, 0, false);
                });
    }

    private Enrollment enrollment(Formation formation, EnrollmentStatus status) {
        return Enrollment.builder()
                .id(99L)
                .learner(learner("backend", LearnerLevel.BEGINNER, Set.of()))
                .session(TrainingSession.builder()
                        .id(88L)
                        .formation(formation)
                        .title(formation.getTitle() + " Session")
                        .startDate(LocalDateTime.now())
                        .endDate(LocalDateTime.now().plusHours(4))
                        .capacity(10)
                        .online(true)
                        .status(SessionStatus.OPEN)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build())
                .status(status)
                .enrolledAt(LocalDateTime.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private LearnerProfile learner(String goals, LearnerLevel level, Set<Skill> skills) {
        return LearnerProfile.builder()
                .id(5L)
                .user(User.builder()
                        .id(5L)
                        .firstName("Learner")
                        .lastName("One")
                        .email("learner@test.com")
                        .password("encoded")
                        .role(Role.LEARNER)
                        .enabled(true)
                        .failedLoginAttempts(0)
                        .accountLocked(false)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build())
                .currentLevel(level)
                .skills(new LinkedHashSet<>(skills))
                .learningGoals(goals)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private Formation formation(Long id, String title, FormationLevel level, Set<Skill> requiredSkills) {
        return Formation.builder()
                .id(id)
                .title(title)
                .description(title)
                .price(BigDecimal.valueOf(200))
                .level(level)
                .durationHours(24)
                .active(true)
                .category(Category.builder()
                        .id(1L)
                        .name(title.contains("Communication") ? "Management" : "IT")
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build())
                .requiredSkills(new LinkedHashSet<>(requiredSkills))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private Skill skill(Long id, String name) {
        return Skill.builder()
                .id(id)
                .name(name)
                .description(name)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
