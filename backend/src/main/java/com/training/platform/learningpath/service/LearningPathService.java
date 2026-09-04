package com.training.platform.learningpath.service;

import com.training.platform.catalogue.formation.entity.Formation;
import com.training.platform.catalogue.formation.entity.FormationLevel;
import com.training.platform.catalogue.formation.repository.FormationRepository;
import com.training.platform.enrollment.entity.Enrollment;
import com.training.platform.enrollment.entity.EnrollmentStatus;
import com.training.platform.enrollment.repository.EnrollmentRepository;
import com.training.platform.learner.entity.LearnerLevel;
import com.training.platform.learner.entity.LearnerProfile;
import com.training.platform.learner.repository.LearnerProfileRepository;
import com.training.platform.common.exception.ResourceNotFoundException;
import com.training.platform.learningpath.dto.LearningPathResponse;
import com.training.platform.learningpath.dto.LearningPathStepResponse;
import com.training.platform.learningpath.dto.LearningPathStepStatus;
import com.training.platform.formation.dto.FormationProgressSnapshot;
import com.training.platform.formation.service.FormationProgressService;
import com.training.platform.session.entity.SessionStatus;
import com.training.platform.session.repository.TrainingSessionRepository;
import com.training.platform.skill.entity.Skill;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class LearningPathService {

    private final LearnerProfileRepository learnerProfileRepository;
    private final FormationRepository formationRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TrainingSessionRepository sessionRepository;
    private final FormationProgressService formationProgressService;

    public LearningPathService(
            LearnerProfileRepository learnerProfileRepository,
            FormationRepository formationRepository,
            EnrollmentRepository enrollmentRepository,
            TrainingSessionRepository sessionRepository,
            FormationProgressService formationProgressService
    ) {
        this.learnerProfileRepository = learnerProfileRepository;
        this.formationRepository = formationRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.sessionRepository = sessionRepository;
        this.formationProgressService = formationProgressService;
    }

    @Transactional(readOnly = true)
    public LearningPathResponse generate(String learnerEmail) {
        LearnerProfile learner = learnerProfileRepository.findByUserEmail(learnerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Learner profile not found for current user"));
        List<Enrollment> enrollments = enrollmentRepository.findByLearnerUserEmailOrderByEnrolledAtDesc(learnerEmail);
        List<FormationScore> scoredFormations = formationRepository.findByActiveTrue().stream()
                .map(formation -> scoreFormation(learner, formation, enrollments))
                .sorted(pathComparator())
                .toList();

        Long recommendedFormationId = findRecommendedFormationId(scoredFormations);
        List<FormationScore> personalPath = personalPath(scoredFormations, recommendedFormationId);
        List<LearningPathStepResponse> steps = new ArrayList<>();
        for (int i = 0; i < personalPath.size(); i++) {
            FormationScore score = personalPath.get(i);
            LearningPathStepStatus status = status(score, recommendedFormationId);
            steps.add(toStep(i + 1, score, status));
        }

        int completedSteps = (int) steps.stream().filter(step -> step.status() == LearningPathStepStatus.COMPLETED).count();
        int totalSteps = steps.size();
        int progress = totalSteps == 0
                ? 0
                : (int) Math.round(steps.stream().mapToInt(LearningPathStepResponse::formationProgressPercentage).average().orElse(0));
        int estimatedHours = steps.stream()
                .map(LearningPathStepResponse::durationHours)
                .filter(hours -> hours != null)
                .mapToInt(Integer::intValue)
                .sum();
        LearningPathStepResponse next = steps.stream()
                .filter(step -> step.status() == LearningPathStepStatus.RECOMMENDED_NEXT)
                .findFirst()
                .orElse(null);

        return new LearningPathResponse(
                learner.getId(),
                learner.getUser().getFirstName() + " " + learner.getUser().getLastName(),
                learner.getLearningGoals(),
                learner.getCurrentLevel(),
                progress,
                estimatedHours,
                completedSteps,
                totalSteps,
                next == null ? null : next.formationId(),
                next == null ? null : next.formationTitle(),
                steps
        );
    }

    private FormationScore scoreFormation(LearnerProfile learner, Formation formation, List<Enrollment> enrollments) {
        List<String> learnerSkills = skillNames(learner.getSkills());
        List<String> requiredSkills = skillNames(formation.getRequiredSkills());
        Set<String> learnerSkillKeys = learnerSkills.stream().map(this::normalize).collect(Collectors.toSet());
        List<String> matchingSkills = requiredSkills.stream()
                .filter(skill -> learnerSkillKeys.contains(normalize(skill)))
                .toList();
        List<String> missingSkills = requiredSkills.stream()
                .filter(skill -> !learnerSkillKeys.contains(normalize(skill)))
                .toList();
        int matchPercentage = requiredSkills.isEmpty()
                ? 100
                : (int) Math.round(matchingSkills.size() * 100.0 / requiredSkills.size());
        FormationProgressSnapshot formationProgress = formationProgressService.snapshot(
                learner.getId(),
                formation.getId(),
                enrollments
        );
        boolean completed = formationProgress.formationComplete();
        boolean inProgress = !completed && (formationProgress.completedSessions() > 0 || hasEnrollmentStatus(enrollments, formation.getId(), EnrollmentStatus.CONFIRMED));
        boolean goalMatch = goalMatches(learner, formation, requiredSkills);
        boolean levelMatch = levelMatches(learner.getCurrentLevel(), formation.getLevel());
        boolean hasAvailableSession = sessionRepository.existsByFormationIdAndStatusIn(
                formation.getId(),
                List.of(SessionStatus.OPEN, SessionStatus.PLANNED)
        );
        int businessScore = 0;
        if (goalMatch) businessScore += 40;
        if (levelMatch) businessScore += 20;
        businessScore += matchPercentage / 2;
        if (hasAvailableSession) businessScore += 10;

        return new FormationScore(
                formation,
                requiredSkills,
                matchingSkills,
                missingSkills,
                matchPercentage,
                completed,
                inProgress,
                goalMatch,
                levelMatch,
                hasAvailableSession,
                businessScore,
                formationProgress
        );
    }

    private Comparator<FormationScore> pathComparator() {
        return Comparator
                .comparing(FormationScore::completed).reversed()
                .thenComparing(FormationScore::goalMatch, Comparator.reverseOrder())
                .thenComparingInt(score -> levelOrder(score.formation().getLevel()))
                .thenComparing(FormationScore::matchPercentage, Comparator.reverseOrder())
                .thenComparing(FormationScore::hasAvailableSession, Comparator.reverseOrder());
    }

    private List<FormationScore> personalPath(List<FormationScore> scored, Long recommendedFormationId) {
        List<FormationScore> path = new ArrayList<>();
        Set<Long> used = new java.util.LinkedHashSet<>();

        for (FormationScore score : scored) {
            if (score.completed() && used.add(score.formation().getId())) {
                path.add(score);
            }
        }
        for (FormationScore score : scored) {
            if (!score.completed() && score.inProgress() && used.add(score.formation().getId())) {
                path.add(score);
            }
        }
        scored.stream()
                .filter(score -> score.formation().getId().equals(recommendedFormationId))
                .findFirst()
                .ifPresent(score -> {
                    if (used.add(score.formation().getId())) {
                        path.add(score);
                    }
                });
        scored.stream()
                .filter(score -> !used.contains(score.formation().getId()))
                .filter(score -> score.goalMatch() || (score.levelMatch() && score.matchPercentage() >= 40))
                .sorted(Comparator
                        .comparingInt((FormationScore score) -> levelOrder(score.formation().getLevel()))
                        .thenComparing(FormationScore::matchPercentage, Comparator.reverseOrder()))
                .limit(2)
                .forEach(score -> {
                    if (used.add(score.formation().getId())) {
                        path.add(score);
                    }
                });
        scored.stream()
                .filter(score -> !used.contains(score.formation().getId()))
                .filter(score -> status(score, recommendedFormationId) == LearningPathStepStatus.LOCKED)
                .findFirst()
                .ifPresent(path::add);

        return path;
    }

    private Long findRecommendedFormationId(List<FormationScore> scores) {
        return scores.stream()
                .filter(score -> !score.completed())
                .max(Comparator
                        .comparingInt(FormationScore::businessScore)
                        .thenComparing(FormationScore::matchPercentage))
                .map(score -> score.formation().getId())
                .orElse(null);
    }

    private LearningPathStepStatus status(FormationScore score, Long recommendedFormationId) {
        if (score.completed()) {
            return LearningPathStepStatus.COMPLETED;
        }
        if (score.inProgress()) {
            return LearningPathStepStatus.IN_PROGRESS;
        }
        if (score.formation().getId().equals(recommendedFormationId)) {
            return LearningPathStepStatus.RECOMMENDED_NEXT;
        }
        if (score.matchPercentage() >= 40 || score.levelMatch()) {
            return LearningPathStepStatus.AVAILABLE;
        }
        if (score.matchPercentage() < 40 && score.formation().getLevel() == FormationLevel.ADVANCED) {
            return LearningPathStepStatus.LOCKED;
        }
        return LearningPathStepStatus.AVAILABLE;
    }

    private LearningPathStepResponse toStep(int order, FormationScore score, LearningPathStepStatus status) {
        FormationProgressSnapshot progress = score.formationProgress();
        return new LearningPathStepResponse(
                order,
                score.formation().getId(),
                score.formation().getTitle(),
                score.formation().getCategory().getName(),
                score.formation().getLevel(),
                score.formation().getDurationHours(),
                status,
                score.matchPercentage(),
                score.requiredSkills(),
                score.matchingSkills(),
                score.missingSkills(),
                score.hasAvailableSession(),
                reason(status, progress),
                progress.progressPercentage(),
                progress.completedSessions(),
                progress.totalSessions()
        );
    }

    private String reason(LearningPathStepStatus status, FormationProgressSnapshot progress) {
        return switch (status) {
            case COMPLETED -> "All sessions completed";
            case IN_PROGRESS -> progress.totalSessions() > 0
                    ? progress.completedSessions() + " of " + progress.totalSessions() + " sessions completed"
                    : "Currently in progress";
            case RECOMMENDED_NEXT -> "Best next step based on your goal and skills";
            case AVAILABLE -> "Available because your profile matches the prerequisites";
            case LOCKED -> "Locked until you improve missing skills";
        };
    }

    private boolean hasEnrollmentStatus(List<Enrollment> enrollments, Long formationId, EnrollmentStatus status) {
        return enrollments.stream()
                .anyMatch(enrollment ->
                        enrollment.getSession().getFormation().getId().equals(formationId)
                                && enrollment.getStatus() == status
                );
    }

    private boolean goalMatches(LearnerProfile learner, Formation formation, List<String> requiredSkills) {
        String goals = normalize(learner.getLearningGoals());
        if (!StringUtils.hasText(goals)) {
            return false;
        }
        String title = normalize(formation.getTitle());
        String category = normalize(formation.getCategory().getName());
        if (goals.contains(title) || title.contains(goals) || goals.contains(category) || category.contains(goals)) {
            return true;
        }
        return requiredSkills.stream().map(this::normalize).anyMatch(skill -> goals.contains(skill) || skill.contains(goals));
    }

    private boolean levelMatches(LearnerLevel learnerLevel, FormationLevel formationLevel) {
        return learnerLevel != null && formationLevel != null && learnerLevel.name().equals(formationLevel.name());
    }

    private int levelOrder(FormationLevel level) {
        if (level == null) {
            return 99;
        }
        return switch (level) {
            case BEGINNER -> 1;
            case INTERMEDIATE -> 2;
            case ADVANCED -> 3;
        };
    }

    private List<String> skillNames(Set<Skill> skills) {
        if (skills == null) {
            return List.of();
        }
        return skills.stream().map(Skill::getName).sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace("frensh", "french")
                .replace("francais", "french");
    }

    private record FormationScore(
            Formation formation,
            List<String> requiredSkills,
            List<String> matchingSkills,
            List<String> missingSkills,
            int matchPercentage,
            boolean completed,
            boolean inProgress,
            boolean goalMatch,
            boolean levelMatch,
            boolean hasAvailableSession,
            int businessScore,
            FormationProgressSnapshot formationProgress
    ) {
    }
}
