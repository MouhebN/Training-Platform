package com.training.platform.learner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.training.platform.catalogue.category.entity.Category;
import com.training.platform.catalogue.formation.entity.Formation;
import com.training.platform.catalogue.formation.entity.FormationLevel;
import com.training.platform.catalogue.formation.repository.FormationRepository;
import com.training.platform.catalogue.formation.service.FormationService;
import com.training.platform.learner.dto.ImprovementPlanResponse;
import com.training.platform.learner.dto.ImprovementPriority;
import com.training.platform.learner.dto.LearnerProfileScoreResponse;
import com.training.platform.learner.dto.SkillGapAnalysisResponse;
import com.training.platform.formation.dto.FormationProgressSnapshot;
import com.training.platform.formation.service.FormationProgressService;
import com.training.platform.learner.entity.LearnerLevel;
import com.training.platform.learner.entity.LearnerProfile;
import com.training.platform.session.entity.SessionStatus;
import com.training.platform.session.repository.TrainingSessionRepository;
import com.training.platform.skill.entity.Skill;
import com.training.platform.user.entity.Role;
import com.training.platform.user.entity.User;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LearnerIntelligenceServiceTest {

    @Mock
    private LearnerProfileService learnerProfileService;

    @Mock
    private FormationService formationService;

    @Mock
    private FormationRepository formationRepository;

    @Mock
    private TrainingSessionRepository trainingSessionRepository;

    @Mock
    private com.training.platform.enrollment.repository.EnrollmentRepository enrollmentRepository;

    @Mock
    private FormationProgressService formationProgressService;

    @Mock
    private com.training.platform.ml.service.MlSuggestionClient mlSuggestionClient;

    private LearnerIntelligenceService intelligenceService;

    @BeforeEach
    void setUp() {
        intelligenceService = new LearnerIntelligenceService(
                learnerProfileService,
                formationService,
                formationRepository,
                trainingSessionRepository,
                enrollmentRepository,
                formationProgressService,
                mlSuggestionClient
        );
    }

    @Test
    void profileScoreCalculatesCompletedAndMissingFields() {
        LearnerProfile profile = learner(Set.of(skill(1L, "Java")));
        profile.setPhone(null);
        profile.setLearningGoals(null);

        when(learnerProfileService.getByUserEmail("learner@test.com")).thenReturn(profile);

        LearnerProfileScoreResponse response = intelligenceService.profileScore("learner@test.com");

        assertThat(response.score()).isEqualTo(65);
        assertThat(response.completedFields()).contains("name", "bio", "currentLevel", "skills");
        assertThat(response.missingFields()).contains("phone", "learningGoals");
    }

    @Test
    void skillGapComparesLearnerAndFormationRequiredSkills() {
        LearnerProfile learner = learner(Set.of(skill(1L, "Java")));
        Formation formation = formation(10L, "Spring Boot Fundamentals", Set.of(
                skill(1L, "Java"),
                skill(2L, "Spring Boot")
        ));

        when(learnerProfileService.getByUserEmail("learner@test.com")).thenReturn(learner);
        when(formationService.getFormation(10L)).thenReturn(formation);

        SkillGapAnalysisResponse response = intelligenceService.skillGap("learner@test.com", 10L);

        assertThat(response.matchPercentage()).isEqualTo(50);
        assertThat(response.ready()).isFalse();
        assertThat(response.matchingSkills()).containsExactly("Java");
        assertThat(response.missingSkills()).containsExactly("Spring Boot");
    }

    @Test
    void improvementPlanReturnsHighPriorityGoalMatches() {
        LearnerProfile learner = learner(Set.of(skill(1L, "Java")));
        learner.setLearningGoals("spring backend");
        Formation spring = formation(10L, "Spring Boot Fundamentals", Set.of(skill(1L, "Java"), skill(2L, "Spring Boot")));
        Formation angular = formation(11L, "Angular Essentials", Set.of(skill(3L, "Angular")));

        when(learnerProfileService.getByUserEmail("learner@test.com")).thenReturn(learner);
        when(formationRepository.findTop20ByActiveTrueOrderByCreatedAtDesc()).thenReturn(List.of(angular, spring));
        when(formationRepository.findByActiveTrue()).thenReturn(List.of(angular, spring));
        when(formationProgressService.snapshot(5L, 10L)).thenReturn(new FormationProgressSnapshot(0, 0, 0, false));
        when(formationProgressService.snapshot(5L, 11L)).thenReturn(new FormationProgressSnapshot(0, 0, 0, false));
        when(trainingSessionRepository.existsByFormationIdAndStatusIn(anyLong(), eq(List.of(SessionStatus.OPEN, SessionStatus.PLANNED)))).thenReturn(false);
        when(trainingSessionRepository.existsByFormationIdAndStatusIn(eq(10L), eq(List.of(SessionStatus.OPEN, SessionStatus.PLANNED)))).thenReturn(true);
        when(mlSuggestionClient.suggest(any())).thenReturn(java.util.Optional.empty());

        ImprovementPlanResponse response = intelligenceService.improvementPlan("learner@test.com");

        assertThat(response.suggestionSource()).isEqualTo("RULES");
        assertThat(response.suggestions()).hasSize(1);
        assertThat(response.suggestions().getFirst().formationTitle()).isEqualTo("Spring Boot Fundamentals");
        assertThat(response.suggestions().getFirst().priority()).isEqualTo(ImprovementPriority.HIGH);
        assertThat(response.suggestions().getFirst().reasons()).contains("A planned or open session is available.");
    }

    @Test
    void improvementPlanMarksCompletedFormationsAsDoneAfterRecommendations() {
        LearnerProfile learner = learner(Set.of(skill(1L, "Java")));
        learner.setLearningGoals("spring backend");
        Formation spring = formation(10L, "Spring Boot Fundamentals", Set.of(skill(1L, "Java"), skill(2L, "Spring Boot")));
        Formation angular = formation(11L, "Angular Essentials", Set.of(skill(3L, "Angular")));
        com.training.platform.enrollment.entity.Enrollment completed = com.training.platform.enrollment.entity.Enrollment.builder()
                .id(1L)
                .learner(learner)
                .session(com.training.platform.session.entity.TrainingSession.builder()
                        .id(50L)
                        .formation(spring)
                        .title("Spring Boot July")
                        .startDate(java.time.LocalDateTime.now().minusDays(3))
                        .endDate(java.time.LocalDateTime.now().minusDays(1))
                        .capacity(10)
                        .online(true)
                        .status(SessionStatus.COMPLETED)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build())
                .status(com.training.platform.enrollment.entity.EnrollmentStatus.COMPLETED)
                .enrolledAt(java.time.LocalDateTime.now().minusDays(3))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(learnerProfileService.getByUserEmail("learner@test.com")).thenReturn(learner);
        when(formationRepository.findTop20ByActiveTrueOrderByCreatedAtDesc()).thenReturn(List.of(angular, spring));
        when(formationRepository.findByActiveTrue()).thenReturn(List.of(angular, spring));
        when(formationProgressService.snapshot(5L, 10L)).thenReturn(new FormationProgressSnapshot(1, 1, 100, true));
        when(formationProgressService.snapshot(5L, 11L)).thenReturn(new FormationProgressSnapshot(0, 0, 0, false));
        when(trainingSessionRepository.existsByFormationIdAndStatusIn(anyLong(), eq(List.of(SessionStatus.OPEN, SessionStatus.PLANNED)))).thenReturn(false);
        when(mlSuggestionClient.suggest(any())).thenReturn(java.util.Optional.empty());

        ImprovementPlanResponse response = intelligenceService.improvementPlan("learner@test.com");

        assertThat(response.suggestions()).hasSize(1);
        assertThat(response.suggestions().getFirst().formationTitle()).isEqualTo("Spring Boot Fundamentals");
        assertThat(response.suggestions().getFirst().priority()).isEqualTo(ImprovementPriority.DONE);
        assertThat(response.suggestions().getFirst().reasons()).contains("You completed all sessions of this formation.");
        assertThat(response.message()).contains("No formation currently suited");
    }

    private LearnerProfile learner(Set<Skill> skills) {
        return LearnerProfile.builder()
                .id(5L)
                .user(user())
                .phone("22222222")
                .bio("Backend learner")
                .currentLevel(LearnerLevel.BEGINNER)
                .skills(new LinkedHashSet<>(skills))
                .learningGoals("backend")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private Formation formation(Long id, String title, Set<Skill> requiredSkills) {
        return Formation.builder()
                .id(id)
                .title(title)
                .description(title)
                .price(BigDecimal.valueOf(200))
                .level(FormationLevel.BEGINNER)
                .durationHours(20)
                .active(true)
                .category(Category.builder().id(1L).name("IT").createdAt(Instant.now()).updatedAt(Instant.now()).build())
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

    private User user() {
        return User.builder()
                .id(8L)
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
                .build();
    }
}
