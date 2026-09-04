package com.training.platform.adminintelligence.service;

import com.training.platform.adminintelligence.dto.AdminAlertResponse;
import com.training.platform.adminintelligence.dto.AdminFormationDemandResponse;
import com.training.platform.adminintelligence.dto.AdminIntelligenceDashboardResponse;
import com.training.platform.adminintelligence.dto.AdminIntelligenceSeverity;
import com.training.platform.adminintelligence.dto.AdminLearnerSignalResponse;
import com.training.platform.adminintelligence.dto.AdminRecommendedActionResponse;
import com.training.platform.adminintelligence.dto.AdminSessionCapacityResponse;
import com.training.platform.adminintelligence.dto.AdminSmartCardResponse;
import com.training.platform.adminintelligence.dto.AdminTrainerLoadResponse;
import com.training.platform.catalogue.formation.entity.Formation;
import com.training.platform.catalogue.formation.repository.FormationRepository;
import com.training.platform.enrollment.entity.Enrollment;
import com.training.platform.enrollment.entity.EnrollmentStatus;
import com.training.platform.enrollment.repository.EnrollmentRepository;
import com.training.platform.learner.entity.LearnerProfile;
import com.training.platform.learner.repository.LearnerProfileRepository;
import com.training.platform.planning.dto.WorkloadLevel;
import com.training.platform.session.entity.SessionStatus;
import com.training.platform.session.entity.TrainingSession;
import com.training.platform.session.repository.TrainingSessionRepository;
import com.training.platform.trainer.entity.TrainerProfile;
import com.training.platform.trainer.repository.TrainerProfileRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminIntelligenceService {

    private static final List<SessionStatus> AVAILABLE_SESSION_STATUSES = List.of(SessionStatus.OPEN, SessionStatus.PLANNED);

    private final FormationRepository formationRepository;
    private final TrainingSessionRepository sessionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TrainerProfileRepository trainerRepository;
    private final LearnerProfileRepository learnerRepository;

    public AdminIntelligenceService(
            FormationRepository formationRepository,
            TrainingSessionRepository sessionRepository,
            EnrollmentRepository enrollmentRepository,
            TrainerProfileRepository trainerRepository,
            LearnerProfileRepository learnerRepository
    ) {
        this.formationRepository = formationRepository;
        this.sessionRepository = sessionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.trainerRepository = trainerRepository;
        this.learnerRepository = learnerRepository;
    }

    @Transactional(readOnly = true)
    public AdminIntelligenceDashboardResponse dashboard() {
        List<Formation> formations = formationRepository.findAll();
        List<TrainingSession> sessions = sessionRepository.findAll();
        List<Enrollment> enrollments = enrollmentRepository.findAll();
        List<TrainerProfile> trainers = trainerRepository.findAll();
        List<LearnerProfile> learners = learnerRepository.findAll();

        Map<Long, List<Enrollment>> enrollmentsByFormation = enrollments.stream()
                .collect(Collectors.groupingBy(enrollment -> enrollment.getSession().getFormation().getId()));
        Map<Long, Long> openSessionsByFormation = sessions.stream()
                .filter(session -> AVAILABLE_SESSION_STATUSES.contains(session.getStatus()))
                .collect(Collectors.groupingBy(session -> session.getFormation().getId(), Collectors.counting()));

        List<AdminFormationDemandResponse> highDemand = highDemandFormations(formations, enrollmentsByFormation, openSessionsByFormation);
        List<AdminFormationDemandResponse> withoutOpenSessions = formationsWithoutOpenSessions(formations, enrollmentsByFormation, openSessionsByFormation);
        List<AdminSessionCapacityResponse> capacityRisks = fullOrAlmostFullSessions(sessions);
        List<AdminTrainerLoadResponse> overloaded = overloadedTrainers(trainers);
        List<AdminLearnerSignalResponse> closeToCertification = learnersCloseToCertification(learners, enrollments);
        List<AdminLearnerSignalResponse> incompleteProfiles = incompleteProfiles(learners);
        List<AdminAlertResponse> alerts = alerts(highDemand, withoutOpenSessions, capacityRisks, overloaded, closeToCertification, incompleteProfiles);
        List<AdminRecommendedActionResponse> actions = actions(withoutOpenSessions, capacityRisks, overloaded, incompleteProfiles);

        return new AdminIntelligenceDashboardResponse(
                Instant.now(),
                smartCards(highDemand, withoutOpenSessions, capacityRisks, overloaded, closeToCertification, incompleteProfiles),
                alerts,
                actions,
                highDemand,
                withoutOpenSessions,
                capacityRisks,
                overloaded,
                closeToCertification,
                incompleteProfiles
        );
    }

    private List<AdminFormationDemandResponse> highDemandFormations(
            List<Formation> formations,
            Map<Long, List<Enrollment>> enrollmentsByFormation,
            Map<Long, Long> openSessionsByFormation
    ) {
        return formations.stream()
                .map(formation -> formationDemand(formation, enrollmentsByFormation, openSessionsByFormation))
                .filter(demand -> demand.enrollmentCount() >= 2 || demand.waitlistedCount() > 0)
                .sorted(Comparator
                        .comparing(AdminFormationDemandResponse::waitlistedCount).reversed()
                        .thenComparing(AdminFormationDemandResponse::enrollmentCount, Comparator.reverseOrder()))
                .limit(5)
                .toList();
    }

    private List<AdminFormationDemandResponse> formationsWithoutOpenSessions(
            List<Formation> formations,
            Map<Long, List<Enrollment>> enrollmentsByFormation,
            Map<Long, Long> openSessionsByFormation
    ) {
        return formations.stream()
                .filter(Formation::getActive)
                .filter(formation -> openSessionsByFormation.getOrDefault(formation.getId(), 0L) == 0)
                .map(formation -> formationDemand(formation, enrollmentsByFormation, openSessionsByFormation))
                .sorted(Comparator.comparing(AdminFormationDemandResponse::enrollmentCount).reversed())
                .limit(8)
                .toList();
    }

    private AdminFormationDemandResponse formationDemand(
            Formation formation,
            Map<Long, List<Enrollment>> enrollmentsByFormation,
            Map<Long, Long> openSessionsByFormation
    ) {
        List<Enrollment> formationEnrollments = enrollmentsByFormation.getOrDefault(formation.getId(), List.of());
        long waitlisted = formationEnrollments.stream().filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.WAITLISTED).count();
        return new AdminFormationDemandResponse(
                formation.getId(),
                formation.getTitle(),
                formation.getCategory().getName(),
                formationEnrollments.size(),
                waitlisted,
                openSessionsByFormation.getOrDefault(formation.getId(), 0L)
        );
    }

    private List<AdminSessionCapacityResponse> fullOrAlmostFullSessions(List<TrainingSession> sessions) {
        return sessions.stream()
                .filter(session -> AVAILABLE_SESSION_STATUSES.contains(session.getStatus()))
                .map(this::capacity)
                .filter(capacity -> capacity.occupancyPercentage() >= 80)
                .sorted(Comparator.comparing(AdminSessionCapacityResponse::occupancyPercentage).reversed())
                .limit(8)
                .toList();
    }

    private AdminSessionCapacityResponse capacity(TrainingSession session) {
        long confirmed = enrollmentRepository.countBySessionIdAndStatusIn(
                session.getId(),
                List.of(EnrollmentStatus.CONFIRMED, EnrollmentStatus.COMPLETED)
        );
        int percentage = session.getCapacity() == null || session.getCapacity() == 0
                ? 0
                : (int) Math.round(confirmed * 100.0 / session.getCapacity());
        return new AdminSessionCapacityResponse(
                session.getId(),
                session.getTitle(),
                session.getFormation().getTitle(),
                session.getCapacity(),
                confirmed,
                percentage,
                confirmed >= session.getCapacity()
        );
    }

    private List<AdminTrainerLoadResponse> overloadedTrainers(List<TrainerProfile> trainers) {
        return trainers.stream()
                .map(this::trainerLoad)
                .filter(load -> load.workloadLevel() == WorkloadLevel.HIGH || load.workloadLevel() == WorkloadLevel.OVERLOADED)
                .sorted(Comparator.comparing(AdminTrainerLoadResponse::next30DaysHours).reversed())
                .toList();
    }

    private AdminTrainerLoadResponse trainerLoad(TrainerProfile trainer) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime in30Days = now.plusDays(30);
        List<TrainingSession> upcoming = sessionRepository.findByTrainerIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                trainer.getId(),
                in30Days,
                now
        ).stream().filter(session -> session.getStatus() != SessionStatus.CANCELLED).toList();
        long hours = upcoming.stream().mapToLong(session -> Math.max(1, Duration.between(session.getStartDate(), session.getEndDate()).toHours())).sum();
        return new AdminTrainerLoadResponse(
                trainer.getId(),
                trainer.getUser().getFirstName() + " " + trainer.getUser().getLastName(),
                trainer.getUser().getEmail(),
                hours,
                upcoming.size(),
                workloadLevel(hours)
        );
    }

    private List<AdminLearnerSignalResponse> learnersCloseToCertification(List<LearnerProfile> learners, List<Enrollment> enrollments) {
        Map<Long, List<Enrollment>> byLearner = enrollments.stream().collect(Collectors.groupingBy(enrollment -> enrollment.getLearner().getId()));
        long activeFormationCount = Math.max(1, formationRepository.findByActiveTrue().size());
        return learners.stream()
                .map(learner -> {
                    long completed = byLearner.getOrDefault(learner.getId(), List.of()).stream()
                            .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.COMPLETED)
                            .map(enrollment -> enrollment.getSession().getFormation().getId())
                            .distinct()
                            .count();
                    int progress = (int) Math.round(completed * 100.0 / activeFormationCount);
                    return learnerSignal(learner, progress, "Learner is close to certification based on completed formations.");
                })
                .filter(signal -> signal.score() >= 60 && signal.score() < 100)
                .sorted(Comparator.comparing(AdminLearnerSignalResponse::score).reversed())
                .limit(8)
                .toList();
    }

    private List<AdminLearnerSignalResponse> incompleteProfiles(List<LearnerProfile> learners) {
        return learners.stream()
                .map(learner -> learnerSignal(learner, profileScore(learner), "Profile is incomplete. Ask learner to add phone, bio, goals, level and skills."))
                .filter(signal -> signal.score() < 80)
                .sorted(Comparator.comparing(AdminLearnerSignalResponse::score))
                .limit(8)
                .toList();
    }

    private List<AdminSmartCardResponse> smartCards(
            List<AdminFormationDemandResponse> highDemand,
            List<AdminFormationDemandResponse> withoutOpenSessions,
            List<AdminSessionCapacityResponse> capacityRisks,
            List<AdminTrainerLoadResponse> overloaded,
            List<AdminLearnerSignalResponse> closeToCertification,
            List<AdminLearnerSignalResponse> incompleteProfiles
    ) {
        return List.of(
                new AdminSmartCardResponse("HIGH_DEMAND", "High-demand formations", String.valueOf(highDemand.size()), "Formations with strong enrollment or waitlist demand", severity(highDemand.size(), 3)),
                new AdminSmartCardResponse("NO_OPEN_SESSIONS", "No open sessions", String.valueOf(withoutOpenSessions.size()), "Active formations needing planned sessions", severity(withoutOpenSessions.size(), 2)),
                new AdminSmartCardResponse("CAPACITY_RISK", "Full or almost full sessions", String.valueOf(capacityRisks.size()), "Sessions at 80% capacity or more", severity(capacityRisks.size(), 2)),
                new AdminSmartCardResponse("OVERLOADED_TRAINERS", "Overloaded trainers", String.valueOf(overloaded.size()), "Trainers with high upcoming workload", severity(overloaded.size(), 1)),
                new AdminSmartCardResponse("CERTIFICATION_READY", "Close to certification", String.valueOf(closeToCertification.size()), "Learners nearing completion", AdminIntelligenceSeverity.INFO),
                new AdminSmartCardResponse("INCOMPLETE_PROFILES", "Incomplete profiles", String.valueOf(incompleteProfiles.size()), "Learners needing better profile data", severity(incompleteProfiles.size(), 4))
        );
    }

    private List<AdminAlertResponse> alerts(
            List<AdminFormationDemandResponse> highDemand,
            List<AdminFormationDemandResponse> withoutOpenSessions,
            List<AdminSessionCapacityResponse> capacityRisks,
            List<AdminTrainerLoadResponse> overloaded,
            List<AdminLearnerSignalResponse> closeToCertification,
            List<AdminLearnerSignalResponse> incompleteProfiles
    ) {
        List<AdminAlertResponse> alerts = new java.util.ArrayList<>();
        highDemand.stream().limit(3).forEach(item -> alerts.add(new AdminAlertResponse(
                "HIGH_DEMAND_FORMATION",
                item.waitlistedCount() > 0 ? AdminIntelligenceSeverity.CRITICAL : AdminIntelligenceSeverity.WARNING,
                "High demand detected",
                item.formationTitle() + " has " + item.enrollmentCount() + " enrollments and " + item.waitlistedCount() + " waitlisted learners.",
                item.formationId(),
                item.formationTitle()
        )));
        withoutOpenSessions.stream().limit(3).forEach(item -> alerts.add(new AdminAlertResponse(
                "FORMATION_WITHOUT_OPEN_SESSION",
                AdminIntelligenceSeverity.WARNING,
                "Formation has no open session",
                item.formationTitle() + " is active but has no OPEN or PLANNED session.",
                item.formationId(),
                item.formationTitle()
        )));
        capacityRisks.stream().limit(3).forEach(item -> alerts.add(new AdminAlertResponse(
                "SESSION_CAPACITY_RISK",
                item.full() ? AdminIntelligenceSeverity.CRITICAL : AdminIntelligenceSeverity.WARNING,
                item.full() ? "Session is full" : "Session almost full",
                item.sessionTitle() + " is at " + item.occupancyPercentage() + "% capacity.",
                item.sessionId(),
                item.sessionTitle()
        )));
        overloaded.stream().limit(3).forEach(item -> alerts.add(new AdminAlertResponse(
                "TRAINER_OVERLOADED",
                item.workloadLevel() == WorkloadLevel.OVERLOADED ? AdminIntelligenceSeverity.CRITICAL : AdminIntelligenceSeverity.WARNING,
                "Trainer workload risk",
                item.trainerFullName() + " has " + item.next30DaysHours() + " planned hours in the next 30 days.",
                item.trainerId(),
                item.trainerFullName()
        )));
        closeToCertification.stream().limit(2).forEach(item -> alerts.add(new AdminAlertResponse(
                "LEARNER_CLOSE_TO_CERTIFICATION",
                AdminIntelligenceSeverity.INFO,
                "Learner close to certification",
                item.learnerFullName() + " is at " + item.score() + "% completion.",
                item.learnerId(),
                item.learnerFullName()
        )));
        incompleteProfiles.stream().limit(3).forEach(item -> alerts.add(new AdminAlertResponse(
                "INCOMPLETE_LEARNER_PROFILE",
                AdminIntelligenceSeverity.INFO,
                "Incomplete learner profile",
                item.learnerFullName() + " profile score is " + item.score() + "%.",
                item.learnerId(),
                item.learnerFullName()
        )));
        return alerts;
    }

    private List<AdminRecommendedActionResponse> actions(
            List<AdminFormationDemandResponse> withoutOpenSessions,
            List<AdminSessionCapacityResponse> capacityRisks,
            List<AdminTrainerLoadResponse> overloaded,
            List<AdminLearnerSignalResponse> incompleteProfiles
    ) {
        List<AdminRecommendedActionResponse> actions = new java.util.ArrayList<>();
        if (!withoutOpenSessions.isEmpty()) {
            actions.add(new AdminRecommendedActionResponse("HIGH", "Plan missing sessions", "Create OPEN or PLANNED sessions for active formations without availability.", "Open sessions", "/admin/sessions"));
        }
        if (!capacityRisks.isEmpty()) {
            actions.add(new AdminRecommendedActionResponse("HIGH", "Increase capacity or duplicate sessions", "Full sessions may generate waitlists. Add capacity or plan another session.", "Review sessions", "/admin/sessions"));
        }
        if (!overloaded.isEmpty()) {
            actions.add(new AdminRecommendedActionResponse("MEDIUM", "Balance trainer workload", "Use trainer workload dashboard before assigning new sessions.", "View workload", "/admin/trainer-workload"));
        }
        if (!incompleteProfiles.isEmpty()) {
            actions.add(new AdminRecommendedActionResponse("LOW", "Improve learner data quality", "Ask learners to complete profiles so recommendations become more accurate.", "View learners", "/admin/users"));
        }
        if (actions.isEmpty()) {
            actions.add(new AdminRecommendedActionResponse("LOW", "Platform is balanced", "No urgent intelligence actions detected today.", "View dashboard", "/admin/dashboard"));
        }
        return actions;
    }

    private AdminLearnerSignalResponse learnerSignal(LearnerProfile learner, int score, String message) {
        return new AdminLearnerSignalResponse(
                learner.getId(),
                learner.getUser().getFirstName() + " " + learner.getUser().getLastName(),
                learner.getUser().getEmail(),
                score,
                message
        );
    }

    private int profileScore(LearnerProfile profile) {
        int score = 0;
        if (StringUtils.hasText(profile.getUser().getFirstName()) && StringUtils.hasText(profile.getUser().getLastName())) score += 15;
        if (StringUtils.hasText(profile.getPhone())) score += 15;
        if (StringUtils.hasText(profile.getBio())) score += 15;
        if (profile.getCurrentLevel() != null) score += 15;
        if (profile.getSkills() != null && !profile.getSkills().isEmpty()) score += 20;
        if (StringUtils.hasText(profile.getLearningGoals())) score += 20;
        return Math.min(100, score);
    }

    private WorkloadLevel workloadLevel(long hours) {
        if (hours <= 5) return WorkloadLevel.LOW;
        if (hours <= 20) return WorkloadLevel.NORMAL;
        if (hours <= 35) return WorkloadLevel.HIGH;
        return WorkloadLevel.OVERLOADED;
    }

    private AdminIntelligenceSeverity severity(int count, int warningThreshold) {
        if (count >= warningThreshold * 2) return AdminIntelligenceSeverity.CRITICAL;
        if (count >= warningThreshold) return AdminIntelligenceSeverity.WARNING;
        return AdminIntelligenceSeverity.INFO;
    }
}
