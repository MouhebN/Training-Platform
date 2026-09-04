package com.training.platform.intelligence.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.training.platform.catalogue.category.entity.Category;
import com.training.platform.catalogue.formation.entity.Formation;
import com.training.platform.catalogue.formation.entity.FormationLevel;
import com.training.platform.catalogue.formation.repository.FormationRepository;
import com.training.platform.enrollment.entity.Enrollment;
import com.training.platform.enrollment.entity.EnrollmentStatus;
import com.training.platform.enrollment.repository.EnrollmentRepository;
import com.training.platform.intelligence.dto.AdminIntelligenceResponse;
import com.training.platform.intelligence.dto.HighDemandFormationResponse;
import com.training.platform.intelligence.dto.RiskLevel;
import com.training.platform.learner.entity.LearnerLevel;
import com.training.platform.learner.entity.LearnerProfile;
import com.training.platform.learner.repository.LearnerProfileRepository;
import com.training.platform.planning.dto.WorkloadLevel;
import com.training.platform.session.entity.SessionStatus;
import com.training.platform.session.entity.TrainingSession;
import com.training.platform.session.repository.TrainingSessionRepository;
import com.training.platform.skill.entity.Skill;
import com.training.platform.trainer.entity.TrainerProfile;
import com.training.platform.trainer.repository.TrainerProfileRepository;
import com.training.platform.user.entity.Role;
import com.training.platform.user.entity.User;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminIntelligenceCenterServiceTest {

    @Mock
    private FormationRepository formationRepository;

    @Mock
    private TrainingSessionRepository sessionRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private TrainerProfileRepository trainerRepository;

    @Mock
    private LearnerProfileRepository learnerRepository;

    private AdminIntelligenceCenterService service;

    @BeforeEach
    void setUp() {
        service = new AdminIntelligenceCenterService(
                formationRepository,
                sessionRepository,
                enrollmentRepository,
                trainerRepository,
                learnerRepository
        );
    }

    @Test
    void dashboardCalculatesGlobalHealthScorePenalties() {
        AdminIntelligenceResponse response = dashboardWithRiskyData();

        assertThat(response.globalHealthScore()).isLessThan(100);
        assertThat(response.summary().overloadedTrainerCount()).isEqualTo(1);
        assertThat(response.summary().highRiskSessionCount()).isEqualTo(1);
        assertThat(response.summary().incompleteLearnerProfileCount()).isEqualTo(1);
    }

    @Test
    void dashboardDetectsHighDemandFormationWithoutOpenSessions() {
        AdminIntelligenceResponse response = dashboardWithRiskyData();

        assertThat(response.highDemandFormations())
                .extracting(HighDemandFormationResponse::formationTitle)
                .contains("Spring Boot Fundamentals");
        assertThat(response.highDemandFormations().getFirst().demandScore()).isGreaterThanOrEqualTo(50);
        assertThat(response.highDemandFormations().getFirst().suggestedAction()).isEqualTo("Create a new session");
    }

    @Test
    void dashboardCalculatesFullSessionRisk() {
        AdminIntelligenceResponse response = dashboardWithRiskyData();

        assertThat(response.sessionRisks()).anySatisfy(risk -> {
            assertThat(risk.sessionTitle()).isEqualTo("Angular July");
            assertThat(risk.riskLevel()).isEqualTo(RiskLevel.FULL);
            assertThat(risk.capacityUsagePercentage()).isEqualTo(100);
        });
    }

    @Test
    void dashboardDetectsOverloadedTrainer() {
        AdminIntelligenceResponse response = dashboardWithRiskyData();

        assertThat(response.overloadedTrainers()).anySatisfy(trainer -> {
            assertThat(trainer.trainerFullName()).isEqualTo("Trainer One");
            assertThat(trainer.workloadLevel()).isEqualTo(WorkloadLevel.OVERLOADED);
            assertThat(trainer.totalHours()).isGreaterThan(35);
        });
    }

    @Test
    void dashboardDetectsLearnerProfileRisk() {
        AdminIntelligenceResponse response = dashboardWithRiskyData();

        assertThat(response.learnerProfileRisks()).anySatisfy(learner -> {
            assertThat(learner.learnerFullName()).isEqualTo("Learner One");
            assertThat(learner.profileScore()).isLessThan(60);
            assertThat(learner.missingFields()).contains("phone", "bio", "skills");
        });
    }

    @Test
    void dashboardCountsTopMissingSkills() {
        AdminIntelligenceResponse response = dashboardWithRiskyData();

        assertThat(response.topMissingSkills()).anySatisfy(skill -> {
            assertThat(skill.skillName()).isEqualTo("Spring Boot");
            assertThat(skill.missingCount()).isEqualTo(1);
            assertThat(skill.relatedFormationCount()).isEqualTo(1);
        });
    }

    private AdminIntelligenceResponse dashboardWithRiskyData() {
        Skill java = skill(1L, "Java");
        Skill spring = skill(2L, "Spring Boot");
        Skill angularSkill = skill(3L, "Angular");

        Formation springFormation = formation(10L, "Spring Boot Fundamentals", "IT", Set.of(java, spring));
        Formation angularFormation = formation(20L, "Angular Essentials", "IT", Set.of(angularSkill));
        TrainerProfile trainer = trainer(1L);
        TrainingSession fullSession = session(30L, angularFormation, trainer, "Angular July", 1, SessionStatus.OPEN, 40);
        LearnerProfile learner = incompleteLearner("spring boot");
        Enrollment enrollment = enrollment(40L, learner, fullSession, EnrollmentStatus.CONFIRMED);

        when(formationRepository.findAll()).thenReturn(List.of(springFormation, angularFormation));
        when(sessionRepository.findAll()).thenReturn(List.of(fullSession));
        when(enrollmentRepository.findAll()).thenReturn(List.of(enrollment));
        when(trainerRepository.findAll()).thenReturn(List.of(trainer));
        when(learnerRepository.findAll()).thenReturn(List.of(learner));

        return service.getIntelligence();
    }

    private Formation formation(Long id, String title, String categoryName, Set<Skill> requiredSkills) {
        return Formation.builder()
                .id(id)
                .title(title)
                .description(title)
                .price(BigDecimal.valueOf(100))
                .level(FormationLevel.BEGINNER)
                .durationHours(20)
                .active(true)
                .category(Category.builder()
                        .id(1L)
                        .name(categoryName)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build())
                .requiredSkills(new LinkedHashSet<>(requiredSkills))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private TrainingSession session(
            Long id,
            Formation formation,
            TrainerProfile trainer,
            String title,
            int capacity,
            SessionStatus status,
            int durationHours
    ) {
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        return TrainingSession.builder()
                .id(id)
                .formation(formation)
                .trainer(trainer)
                .title(title)
                .startDate(start)
                .endDate(start.plusHours(durationHours))
                .capacity(capacity)
                .online(true)
                .status(status)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private Enrollment enrollment(Long id, LearnerProfile learner, TrainingSession session, EnrollmentStatus status) {
        return Enrollment.builder()
                .id(id)
                .learner(learner)
                .session(session)
                .status(status)
                .enrolledAt(LocalDateTime.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private LearnerProfile incompleteLearner(String goals) {
        return LearnerProfile.builder()
                .id(2L)
                .user(user(2L, "Learner", "One", "learner@test.com", Role.LEARNER))
                .currentLevel(LearnerLevel.BEGINNER)
                .skills(new LinkedHashSet<>())
                .learningGoals(goals)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private TrainerProfile trainer(Long id) {
        return TrainerProfile.builder()
                .id(id)
                .user(user(3L, "Trainer", "One", "trainer@test.com", Role.TRAINER))
                .yearsOfExperience(5)
                .averageRating(4.5)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private User user(Long id, String firstName, String lastName, String email, Role role) {
        return User.builder()
                .id(id)
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .password("encoded")
                .role(role)
                .enabled(true)
                .failedLoginAttempts(0)
                .accountLocked(false)
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
