package com.training.platform.intelligence.service;

import com.training.platform.catalogue.formation.entity.Formation;
import com.training.platform.catalogue.formation.repository.FormationRepository;
import com.training.platform.enrollment.entity.Enrollment;
import com.training.platform.enrollment.entity.EnrollmentStatus;
import com.training.platform.enrollment.repository.EnrollmentRepository;
import com.training.platform.intelligence.dto.ActionPriority;
import com.training.platform.intelligence.dto.AdminIntelligenceResponse;
import com.training.platform.intelligence.dto.HighDemandFormationResponse;
import com.training.platform.intelligence.dto.IntelligenceAlertResponse;
import com.training.platform.intelligence.dto.IntelligenceSeverity;
import com.training.platform.intelligence.dto.IntelligenceSummaryResponse;
import com.training.platform.intelligence.dto.LearnerProfileRiskResponse;
import com.training.platform.intelligence.dto.MissingSkillInsightResponse;
import com.training.platform.intelligence.dto.RecommendedActionResponse;
import com.training.platform.intelligence.dto.RiskLevel;
import com.training.platform.intelligence.dto.SessionRiskInsightResponse;
import com.training.platform.intelligence.dto.TrainerWorkloadInsightResponse;
import com.training.platform.learner.entity.LearnerProfile;
import com.training.platform.learner.repository.LearnerProfileRepository;
import com.training.platform.planning.dto.WorkloadLevel;
import com.training.platform.session.entity.SessionStatus;
import com.training.platform.session.entity.TrainingSession;
import com.training.platform.session.repository.TrainingSessionRepository;
import com.training.platform.skill.entity.Skill;
import com.training.platform.trainer.entity.TrainerProfile;
import com.training.platform.trainer.repository.TrainerProfileRepository;
import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminIntelligenceCenterService {

    private static final List<SessionStatus> OPEN_SESSION_STATUSES = List.of(SessionStatus.OPEN, SessionStatus.PLANNED);
    private static final List<SessionStatus> ACTIVE_SESSION_STATUSES = List.of(SessionStatus.OPEN, SessionStatus.PLANNED, SessionStatus.IN_PROGRESS);

    private final FormationRepository formationRepository;
    private final TrainingSessionRepository sessionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TrainerProfileRepository trainerRepository;
    private final LearnerProfileRepository learnerRepository;

    public AdminIntelligenceCenterService(
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

    public AdminIntelligenceResponse getIntelligence() {
        List<Formation> formations = formationRepository.findAll();
        List<Formation> activeFormations = formations.stream()
                .filter(formation -> Boolean.TRUE.equals(formation.getActive()))
                .toList();
        List<TrainingSession> sessions = sessionRepository.findAll();
        List<Enrollment> enrollments = enrollmentRepository.findAll();
        List<TrainerProfile> trainers = trainerRepository.findAll();
        List<LearnerProfile> learners = learnerRepository.findAll();

        List<HighDemandFormationResponse> highDemandFormations = buildHighDemandFormations(activeFormations, sessions, enrollments, learners);
        List<TrainerWorkloadInsightResponse> overloadedTrainers = buildTrainerWorkload(trainers, sessions);
        List<SessionRiskInsightResponse> sessionRisks = buildSessionRisks(sessions, enrollments);
        List<LearnerProfileRiskResponse> learnerProfileRisks = buildLearnerProfileRisks(learners);
        List<MissingSkillInsightResponse> topMissingSkills = buildTopMissingSkills(activeFormations, learners);
        long learnersCloseToCertification = countLearnersCloseToCertification(learners, enrollments);
        List<IntelligenceAlertResponse> alerts = buildAlerts(highDemandFormations, overloadedTrainers, sessionRisks, learnerProfileRisks, learnersCloseToCertification);
        List<RecommendedActionResponse> actions = buildRecommendedActions(highDemandFormations, overloadedTrainers, sessionRisks, learnerProfileRisks, topMissingSkills, learnersCloseToCertification);

        long openSessions = sessions.stream()
                .filter(session -> OPEN_SESSION_STATUSES.contains(session.getStatus()))
                .count();
        long confirmedEnrollments = enrollments.stream()
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.CONFIRMED)
                .count();
        long waitlistedEnrollments = enrollments.stream()
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.WAITLISTED)
                .count();

        IntelligenceSummaryResponse summary = new IntelligenceSummaryResponse(
                activeFormations.size(),
                openSessions,
                confirmedEnrollments,
                waitlistedEnrollments,
                (int) overloadedTrainers.stream().filter(item -> item.workloadLevel() == WorkloadLevel.OVERLOADED).count(),
                (int) sessionRisks.stream().filter(item -> item.riskLevel() == RiskLevel.HIGH || item.riskLevel() == RiskLevel.FULL).count(),
                learnerProfileRisks.size(),
                highDemandFormations.size()
        );

        int globalHealthScore = calculateHealthScore(
                summary,
                highDemandFormations,
                sessionRisks,
                activeFormations
        );

        return new AdminIntelligenceResponse(
                LocalDateTime.now(),
                globalHealthScore,
                summary,
                alerts,
                highDemandFormations,
                overloadedTrainers,
                sessionRisks,
                learnerProfileRisks,
                topMissingSkills,
                actions
        );
    }

    int calculateHealthScore(
            IntelligenceSummaryResponse summary,
            List<HighDemandFormationResponse> highDemandFormations,
            List<SessionRiskInsightResponse> sessionRisks,
            List<Formation> activeFormations
    ) {
        long highDemandWithoutSessions = highDemandFormations.stream()
                .filter(item -> item.availableSessionCount() == 0)
                .count();
        long riskySessions = sessionRisks.stream()
                .filter(item -> item.riskLevel() == RiskLevel.HIGH || item.riskLevel() == RiskLevel.FULL)
                .count();
        long formationsWithoutSkills = activeFormations.stream()
                .filter(formation -> formation.getRequiredSkills() == null || formation.getRequiredSkills().isEmpty())
                .count();

        int score = 100;
        score -= summary.overloadedTrainerCount() * 5;
        score -= riskySessions * 5;
        score -= highDemandWithoutSessions * 3;
        score -= summary.incompleteLearnerProfileCount() * 2;
        score -= formationsWithoutSkills * 2;
        return Math.max(0, score);
    }

    List<HighDemandFormationResponse> buildHighDemandFormations(
            List<Formation> formations,
            List<TrainingSession> sessions,
            List<Enrollment> enrollments,
            List<LearnerProfile> learners
    ) {
        return formations.stream()
                .map(formation -> toHighDemandFormation(formation, sessions, enrollments, learners))
                .filter(item -> item.demandScore() > 0)
                .sorted(Comparator.comparingInt(HighDemandFormationResponse::demandScore).reversed())
                .limit(5)
                .toList();
    }

    private HighDemandFormationResponse toHighDemandFormation(
            Formation formation,
            List<TrainingSession> sessions,
            List<Enrollment> enrollments,
            List<LearnerProfile> learners
    ) {
        List<TrainingSession> formationSessions = sessions.stream()
                .filter(session -> sameId(session.getFormation().getId(), formation.getId()))
                .toList();
        long availableSessions = formationSessions.stream()
                .filter(session -> OPEN_SESSION_STATUSES.contains(session.getStatus()))
                .count();
        long confirmedEnrollments = countEnrollmentsForFormation(enrollments, formation.getId(), Set.of(EnrollmentStatus.CONFIRMED));
        long waitlistedEnrollments = countEnrollmentsForFormation(enrollments, formation.getId(), Set.of(EnrollmentStatus.WAITLISTED));
        int activeCapacity = formationSessions.stream()
                .filter(session -> ACTIVE_SESSION_STATUSES.contains(session.getStatus()))
                .map(TrainingSession::getCapacity)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        long learnersInterested = learners.stream()
                .filter(learner -> learnerGoalMatchesFormation(learner, formation))
                .count();
        long missingSkillOccurrences = missingRequiredSkillOccurrences(formation, learners);

        int score = 0;
        List<String> reasons = new ArrayList<>();
        if (availableSessions == 0) {
            score += 30;
            reasons.add("No open or planned sessions");
        }
        if (activeCapacity > 0 && confirmedEnrollments >= activeCapacity) {
            score += 20;
            reasons.add("Confirmed enrollments reached available capacity");
        }
        if (waitlistedEnrollments > 0) {
            score += 20;
            reasons.add("Learners are waiting for a place");
        }
        if (learnersInterested > 0) {
            score += Math.min(30, (int) learnersInterested * 10);
            reasons.add(learnersInterested + " learner goals match this formation");
        }
        if (missingSkillOccurrences > 0) {
            score += Math.min(20, (int) missingSkillOccurrences * 5);
            reasons.add("Required skills are missing in learner profiles");
        }

        return new HighDemandFormationResponse(
                formation.getId(),
                formation.getTitle(),
                formation.getCategory() != null ? formation.getCategory().getName() : "Uncategorized",
                score,
                learnersInterested,
                availableSessions,
                confirmedEnrollments,
                waitlistedEnrollments,
                reasons.isEmpty() ? "Demand is stable." : String.join(". ", reasons) + ".",
                suggestedAction(availableSessions, waitlistedEnrollments, activeCapacity, confirmedEnrollments)
        );
    }

    List<TrainerWorkloadInsightResponse> buildTrainerWorkload(List<TrainerProfile> trainers, List<TrainingSession> sessions) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime to = now.plusDays(30);
        return trainers.stream()
                .map(trainer -> toTrainerWorkloadInsight(trainer, sessions, now, to))
                .filter(item -> item.workloadLevel() == WorkloadLevel.HIGH || item.workloadLevel() == WorkloadLevel.OVERLOADED)
                .sorted(Comparator
                        .comparing((TrainerWorkloadInsightResponse item) -> item.workloadLevel() == WorkloadLevel.OVERLOADED ? 0 : 1)
                        .thenComparing(TrainerWorkloadInsightResponse::totalHours, Comparator.reverseOrder()))
                .toList();
    }

    private TrainerWorkloadInsightResponse toTrainerWorkloadInsight(
            TrainerProfile trainer,
            List<TrainingSession> sessions,
            LocalDateTime from,
            LocalDateTime to
    ) {
        List<TrainingSession> trainerSessions = sessions.stream()
                .filter(session -> session.getTrainer() != null && sameId(session.getTrainer().getId(), trainer.getId()))
                .filter(session -> session.getStatus() != SessionStatus.CANCELLED)
                .filter(session -> overlaps(session.getStartDate(), session.getEndDate(), from, to))
                .toList();
        long totalHours = trainerSessions.stream()
                .mapToLong(this::durationHours)
                .sum();
        WorkloadLevel level = workloadLevel(totalHours);
        return new TrainerWorkloadInsightResponse(
                trainer.getId(),
                fullName(trainer.getUser().getFirstName(), trainer.getUser().getLastName()),
                trainer.getUser().getEmail(),
                totalHours,
                trainerSessions.size(),
                level,
                workloadReason(level, totalHours),
                workloadAction(level)
        );
    }

    List<SessionRiskInsightResponse> buildSessionRisks(List<TrainingSession> sessions, List<Enrollment> enrollments) {
        return sessions.stream()
                .filter(session -> ACTIVE_SESSION_STATUSES.contains(session.getStatus()))
                .map(session -> toSessionRiskInsight(session, enrollments))
                .filter(item -> item.riskLevel() != RiskLevel.LOW)
                .sorted(Comparator
                        .comparing((SessionRiskInsightResponse item) -> riskRank(item.riskLevel())).reversed()
                        .thenComparing(SessionRiskInsightResponse::capacityUsagePercentage, Comparator.reverseOrder()))
                .toList();
    }

    private SessionRiskInsightResponse toSessionRiskInsight(TrainingSession session, List<Enrollment> enrollments) {
        long confirmed = countEnrollmentsForSession(enrollments, session.getId(), Set.of(EnrollmentStatus.CONFIRMED));
        long waitlisted = countEnrollmentsForSession(enrollments, session.getId(), Set.of(EnrollmentStatus.WAITLISTED));
        int capacity = session.getCapacity() != null ? session.getCapacity() : 0;
        int usage = capacity <= 0 ? 0 : (int) Math.round((confirmed * 100.0) / capacity);
        RiskLevel risk = sessionRiskLevel(usage);
        return new SessionRiskInsightResponse(
                session.getId(),
                sessionTitle(session),
                session.getFormation().getTitle(),
                capacity,
                confirmed,
                waitlisted,
                usage,
                risk,
                sessionRiskReason(risk, usage),
                sessionRiskAction(risk)
        );
    }

    List<LearnerProfileRiskResponse> buildLearnerProfileRisks(List<LearnerProfile> learners) {
        return learners.stream()
                .map(this::toLearnerProfileRisk)
                .filter(item -> item.profileScore() < 60)
                .sorted(Comparator.comparingInt(LearnerProfileRiskResponse::profileScore))
                .limit(10)
                .toList();
    }

    private LearnerProfileRiskResponse toLearnerProfileRisk(LearnerProfile learner) {
        List<String> missing = missingProfileFields(learner);
        int score = 100 - missing.stream().mapToInt(this::profileFieldWeight).sum();
        return new LearnerProfileRiskResponse(
                learner.getId(),
                fullName(learner.getUser().getFirstName(), learner.getUser().getLastName()),
                score,
                missing,
                "Profile score is below 60%, reducing recommendation quality.",
                "Ask learner to complete profile for better recommendations"
        );
    }

    List<MissingSkillInsightResponse> buildTopMissingSkills(List<Formation> formations, List<LearnerProfile> learners) {
        Map<Long, SkillCounter> counters = new LinkedHashMap<>();
        for (Formation formation : formations) {
            if (formation.getRequiredSkills() == null) {
                continue;
            }
            for (Skill skill : formation.getRequiredSkills()) {
                counters.computeIfAbsent(skill.getId(), ignored -> new SkillCounter(skill)).relatedFormationIds.add(formation.getId());
                for (LearnerProfile learner : learners) {
                    if (!hasSkill(learner, skill)) {
                        counters.get(skill.getId()).missingCount++;
                    }
                }
            }
        }

        return counters.values().stream()
                .filter(counter -> counter.missingCount > 0)
                .sorted(Comparator.comparingLong((SkillCounter counter) -> counter.missingCount).reversed())
                .limit(5)
                .map(counter -> new MissingSkillInsightResponse(
                        counter.skill.getId(),
                        counter.skill.getName(),
                        counter.missingCount,
                        counter.relatedFormationIds.size(),
                        counter.missingCount + " learner profiles miss this required skill.",
                        "Create beginner content for this skill"
                ))
                .toList();
    }

    private List<IntelligenceAlertResponse> buildAlerts(
            List<HighDemandFormationResponse> highDemandFormations,
            List<TrainerWorkloadInsightResponse> overloadedTrainers,
            List<SessionRiskInsightResponse> sessionRisks,
            List<LearnerProfileRiskResponse> learnerProfileRisks,
            long learnersCloseToCertification
    ) {
        List<IntelligenceAlertResponse> alerts = new ArrayList<>();
        highDemandFormations.stream()
                .filter(item -> item.demandScore() >= 60)
                .forEach(item -> alerts.add(new IntelligenceAlertResponse(
                        "HIGH_DEMAND_FORMATION",
                        item.demandScore() >= 80 ? IntelligenceSeverity.CRITICAL : IntelligenceSeverity.WARNING,
                        "High demand formation",
                        item.formationTitle() + " has a demand score of " + item.demandScore() + ".",
                        "FORMATION",
                        item.formationId(),
                        item.suggestedAction()
                )));
        overloadedTrainers.forEach(item -> alerts.add(new IntelligenceAlertResponse(
                "TRAINER_WORKLOAD",
                item.workloadLevel() == WorkloadLevel.OVERLOADED ? IntelligenceSeverity.CRITICAL : IntelligenceSeverity.WARNING,
                "Trainer workload risk",
                item.trainerFullName() + " has " + item.totalHours() + " planned hours in the next 30 days.",
                "TRAINER",
                item.trainerId(),
                item.suggestedAction()
        )));
        sessionRisks.stream()
                .filter(item -> item.riskLevel() == RiskLevel.HIGH || item.riskLevel() == RiskLevel.FULL)
                .forEach(item -> alerts.add(new IntelligenceAlertResponse(
                        "SESSION_CAPACITY_RISK",
                        item.riskLevel() == RiskLevel.FULL ? IntelligenceSeverity.CRITICAL : IntelligenceSeverity.WARNING,
                        "Session capacity risk",
                        item.sessionTitle() + " is at " + item.capacityUsagePercentage() + "% capacity.",
                        "SESSION",
                        item.sessionId(),
                        item.suggestedAction()
                )));
        if (!learnerProfileRisks.isEmpty()) {
            alerts.add(new IntelligenceAlertResponse(
                    "INCOMPLETE_LEARNER_PROFILES",
                    IntelligenceSeverity.INFO,
                    "Incomplete learner profiles",
                    learnerProfileRisks.size() + " learners have a profile score below 60%.",
                    "LEARNER",
                    learnerProfileRisks.get(0).learnerId(),
                    "Ask learners to complete profiles"
            ));
        }
        if (learnersCloseToCertification > 0) {
            alerts.add(new IntelligenceAlertResponse(
                    "LEARNERS_CLOSE_TO_CERTIFICATION",
                    IntelligenceSeverity.INFO,
                    "Learners close to certification",
                    learnersCloseToCertification + " learners have completed most of their enrolled training path.",
                    "LEARNER",
                    null,
                    "Prepare certificate validation"
            ));
        }
        return alerts.stream().limit(10).toList();
    }

    private List<RecommendedActionResponse> buildRecommendedActions(
            List<HighDemandFormationResponse> highDemandFormations,
            List<TrainerWorkloadInsightResponse> overloadedTrainers,
            List<SessionRiskInsightResponse> sessionRisks,
            List<LearnerProfileRiskResponse> learnerProfileRisks,
            List<MissingSkillInsightResponse> topMissingSkills,
            long learnersCloseToCertification
    ) {
        List<RecommendedActionResponse> actions = new ArrayList<>();
        highDemandFormations.forEach(item -> actions.add(new RecommendedActionResponse(
                item.demandScore() >= 80 ? ActionPriority.HIGH : ActionPriority.MEDIUM,
                "Plan capacity for " + item.formationTitle(),
                item.reason(),
                "CREATE_SESSION",
                "FORMATION",
                item.formationId(),
                item.suggestedAction()
        )));
        overloadedTrainers.forEach(item -> actions.add(new RecommendedActionResponse(
                item.workloadLevel() == WorkloadLevel.OVERLOADED ? ActionPriority.HIGH : ActionPriority.MEDIUM,
                "Balance workload for " + item.trainerFullName(),
                item.reason(),
                "REBALANCE_TRAINER",
                "TRAINER",
                item.trainerId(),
                item.suggestedAction()
        )));
        sessionRisks.forEach(item -> actions.add(new RecommendedActionResponse(
                item.riskLevel() == RiskLevel.FULL ? ActionPriority.HIGH : ActionPriority.MEDIUM,
                "Reduce session risk",
                item.sessionTitle() + ": " + item.reason(),
                "REVIEW_SESSION_CAPACITY",
                "SESSION",
                item.sessionId(),
                item.suggestedAction()
        )));
        topMissingSkills.stream()
                .filter(item -> item.missingCount() >= 3)
                .forEach(item -> actions.add(new RecommendedActionResponse(
                        ActionPriority.MEDIUM,
                        "Close skill gap: " + item.skillName(),
                        item.reason(),
                        "CREATE_SKILL_CONTENT",
                        "SKILL",
                        item.skillId(),
                        item.suggestedAction()
                )));
        if (!learnerProfileRisks.isEmpty()) {
            actions.add(new RecommendedActionResponse(
                    ActionPriority.LOW,
                    "Improve learner profile quality",
                    learnerProfileRisks.size() + " learner profiles are incomplete.",
                    "PROFILE_COMPLETION_CAMPAIGN",
                    "LEARNER",
                    learnerProfileRisks.get(0).learnerId(),
                    "Ask learner to complete profile"
            ));
        }
        if (learnersCloseToCertification > 0) {
            actions.add(new RecommendedActionResponse(
                    ActionPriority.MEDIUM,
                    "Prepare certification validation",
                    learnersCloseToCertification + " learners are close to certification based on completed enrollments.",
                    "CERTIFICATION_REVIEW",
                    "LEARNER",
                    null,
                    "Review learner completion"
            ));
        }

        return actions.stream()
                .sorted(Comparator.comparingInt(action -> priorityRank(action.priority())))
                .limit(8)
                .toList();
    }

    private long countEnrollmentsForFormation(List<Enrollment> enrollments, Long formationId, Set<EnrollmentStatus> statuses) {
        return enrollments.stream()
                .filter(enrollment -> enrollment.getSession() != null && enrollment.getSession().getFormation() != null)
                .filter(enrollment -> sameId(enrollment.getSession().getFormation().getId(), formationId))
                .filter(enrollment -> statuses.contains(enrollment.getStatus()))
                .count();
    }

    private long countEnrollmentsForSession(List<Enrollment> enrollments, Long sessionId, Set<EnrollmentStatus> statuses) {
        return enrollments.stream()
                .filter(enrollment -> enrollment.getSession() != null)
                .filter(enrollment -> sameId(enrollment.getSession().getId(), sessionId))
                .filter(enrollment -> statuses.contains(enrollment.getStatus()))
                .count();
    }

    private long countLearnersCloseToCertification(List<LearnerProfile> learners, List<Enrollment> enrollments) {
        return learners.stream()
                .filter(learner -> {
                    List<Enrollment> learnerEnrollments = enrollments.stream()
                            .filter(enrollment -> enrollment.getLearner() != null && sameId(enrollment.getLearner().getId(), learner.getId()))
                            .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.CONFIRMED || enrollment.getStatus() == EnrollmentStatus.COMPLETED)
                            .toList();
                    if (learnerEnrollments.isEmpty()) {
                        return false;
                    }
                    long completed = learnerEnrollments.stream()
                            .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.COMPLETED)
                            .count();
                    int progress = (int) Math.round((completed * 100.0) / learnerEnrollments.size());
                    return completed >= 2 || progress >= 75;
                })
                .count();
    }

    private boolean learnerGoalMatchesFormation(LearnerProfile learner, Formation formation) {
        String goals = normalize(learner.getLearningGoals());
        if (goals.isBlank()) {
            return false;
        }
        if (goals.contains(normalize(formation.getTitle()))) {
            return true;
        }
        if (formation.getCategory() != null && goals.contains(normalize(formation.getCategory().getName()))) {
            return true;
        }
        return formation.getRequiredSkills() != null && formation.getRequiredSkills().stream()
                .map(Skill::getName)
                .map(this::normalize)
                .anyMatch(skill -> !skill.isBlank() && goals.contains(skill));
    }

    private long missingRequiredSkillOccurrences(Formation formation, List<LearnerProfile> learners) {
        if (formation.getRequiredSkills() == null || formation.getRequiredSkills().isEmpty()) {
            return 0;
        }
        long missing = 0;
        for (LearnerProfile learner : learners) {
            for (Skill skill : formation.getRequiredSkills()) {
                if (!hasSkill(learner, skill)) {
                    missing++;
                }
            }
        }
        return missing;
    }

    private List<String> missingProfileFields(LearnerProfile learner) {
        List<String> missing = new ArrayList<>();
        if (learner.getUser() == null || isBlank(learner.getUser().getFirstName()) || isBlank(learner.getUser().getLastName())) {
            missing.add("firstName/lastName");
        }
        if (isBlank(learner.getPhone())) {
            missing.add("phone");
        }
        if (isBlank(learner.getBio())) {
            missing.add("bio");
        }
        if (learner.getCurrentLevel() == null) {
            missing.add("currentLevel");
        }
        if (learner.getSkills() == null || learner.getSkills().isEmpty()) {
            missing.add("skills");
        }
        if (isBlank(learner.getLearningGoals())) {
            missing.add("learningGoals");
        }
        return missing;
    }

    private int profileFieldWeight(String field) {
        return switch (field) {
            case "firstName/lastName", "phone", "bio", "currentLevel" -> 15;
            case "skills", "learningGoals" -> 20;
            default -> 0;
        };
    }

    private boolean hasSkill(LearnerProfile learner, Skill requiredSkill) {
        if (learner.getSkills() == null) {
            return false;
        }
        return learner.getSkills().stream().anyMatch(skill -> sameId(skill.getId(), requiredSkill.getId()));
    }

    private WorkloadLevel workloadLevel(long hours) {
        if (hours <= 5) {
            return WorkloadLevel.LOW;
        }
        if (hours <= 20) {
            return WorkloadLevel.NORMAL;
        }
        if (hours <= 35) {
            return WorkloadLevel.HIGH;
        }
        return WorkloadLevel.OVERLOADED;
    }

    private RiskLevel sessionRiskLevel(int usagePercentage) {
        if (usagePercentage >= 100) {
            return RiskLevel.FULL;
        }
        if (usagePercentage >= 80) {
            return RiskLevel.HIGH;
        }
        if (usagePercentage >= 50) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    private String suggestedAction(long availableSessions, long waitlistedEnrollments, int capacity, long confirmedEnrollments) {
        if (availableSessions == 0) {
            return "Create a new session";
        }
        if (waitlistedEnrollments > 0 || (capacity > 0 && confirmedEnrollments >= capacity)) {
            return "Review formation capacity";
        }
        return "Assign available trainer";
    }

    private String workloadReason(WorkloadLevel level, long hours) {
        return switch (level) {
            case LOW -> "Trainer has " + hours + " planned hours and can take more sessions.";
            case NORMAL -> "Trainer workload is balanced at " + hours + " hours.";
            case HIGH -> "Trainer has a high workload with " + hours + " planned hours.";
            case OVERLOADED -> "Trainer is overloaded with " + hours + " planned hours.";
        };
    }

    private String workloadAction(WorkloadLevel level) {
        return switch (level) {
            case LOW -> "Trainer has availability for more sessions.";
            case NORMAL -> "Trainer workload is balanced.";
            case HIGH -> "Avoid assigning too many additional sessions this month";
            case OVERLOADED -> "Reassign future sessions or reduce workload";
        };
    }

    private String sessionRiskReason(RiskLevel level, int usage) {
        return switch (level) {
            case FULL -> "Session is full at " + usage + "% capacity.";
            case HIGH -> "Session is almost full at " + usage + "% capacity.";
            case MEDIUM -> "Session has medium capacity usage at " + usage + "%.";
            case LOW -> "Session has low capacity risk.";
        };
    }

    private String sessionRiskAction(RiskLevel level) {
        return switch (level) {
            case FULL -> "Open another session or increase capacity";
            case HIGH -> "Monitor registrations closely";
            case MEDIUM -> "Continue tracking enrollment";
            case LOW -> "No action needed";
        };
    }

    private long durationHours(TrainingSession session) {
        if (session.getStartDate() == null || session.getEndDate() == null || !session.getEndDate().isAfter(session.getStartDate())) {
            return 0;
        }
        return Math.max(1, Duration.between(session.getStartDate(), session.getEndDate()).toHours());
    }

    private boolean overlaps(LocalDateTime start, LocalDateTime end, LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        return start != null && end != null && start.isBefore(rangeEnd) && end.isAfter(rangeStart);
    }

    private int riskRank(RiskLevel level) {
        return switch (level) {
            case FULL -> 4;
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
    }

    private int priorityRank(ActionPriority priority) {
        return switch (priority) {
            case HIGH -> 0;
            case MEDIUM -> 1;
            case LOW -> 2;
        };
    }

    private String sessionTitle(TrainingSession session) {
        return isBlank(session.getTitle()) ? session.getFormation().getTitle() : session.getTitle();
    }

    private String fullName(String firstName, String lastName) {
        return ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
    }

    private boolean sameId(Long left, Long right) {
        return left != null && left.equals(right);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.toLowerCase(Locale.ROOT).trim();
    }

    private static class SkillCounter {
        private final Skill skill;
        private final Set<Long> relatedFormationIds = new HashSet<>();
        private long missingCount;

        private SkillCounter(Skill skill) {
            this.skill = skill;
        }
    }
}
