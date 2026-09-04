package com.training.platform.planning.service;

import com.training.platform.catalogue.formation.entity.Formation;
import com.training.platform.catalogue.formation.service.FormationService;
import com.training.platform.planning.dto.ConflictSeverity;
import com.training.platform.planning.dto.SessionConflictCheckRequest;
import com.training.platform.planning.dto.SessionConflictCheckResponse;
import com.training.platform.planning.dto.SessionConflictItem;
import com.training.platform.planning.dto.SessionPlanningSuggestionRequest;
import com.training.platform.planning.dto.SessionPlanningSuggestionResponse;
import com.training.platform.planning.dto.TrainerWorkloadResponse;
import com.training.platform.planning.dto.TrainerWorkloadSessionItem;
import com.training.platform.planning.dto.WorkloadLevel;
import com.training.platform.session.entity.SessionStatus;
import com.training.platform.session.entity.TrainingSession;
import com.training.platform.session.repository.TrainingSessionRepository;
import com.training.platform.skill.entity.Skill;
import com.training.platform.trainer.entity.TrainerAvailability;
import com.training.platform.trainer.entity.TrainerProfile;
import com.training.platform.trainer.repository.TrainerAvailabilityRepository;
import com.training.platform.trainer.repository.TrainerProfileRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionPlanningService {

    private static final List<SessionStatus> ACTIVE_SESSION_STATUSES = List.of(
            SessionStatus.PLANNED,
            SessionStatus.OPEN,
            SessionStatus.IN_PROGRESS
    );

    private final FormationService formationService;
    private final TrainerProfileRepository trainerProfileRepository;
    private final TrainerAvailabilityRepository availabilityRepository;
    private final TrainingSessionRepository sessionRepository;

    public SessionPlanningService(
            FormationService formationService,
            TrainerProfileRepository trainerProfileRepository,
            TrainerAvailabilityRepository availabilityRepository,
            TrainingSessionRepository sessionRepository
    ) {
        this.formationService = formationService;
        this.trainerProfileRepository = trainerProfileRepository;
        this.availabilityRepository = availabilityRepository;
        this.sessionRepository = sessionRepository;
    }

    @Transactional(readOnly = true)
    public List<SessionPlanningSuggestionResponse> suggestions(SessionPlanningSuggestionRequest request) {
        Formation formation = formationService.getFormation(request.formationId());

        return trainerProfileRepository.findAll().stream()
                .filter(trainer -> Boolean.TRUE.equals(trainer.getActive()))
                .map(trainer -> bestSuggestion(candidateSuggestions(formation, trainer, request)))
                .flatMap(Optional::stream)
                .sorted(Comparator
                        .comparingInt(SessionPlanningSuggestionResponse::score).reversed()
                        .thenComparing(SessionPlanningSuggestionResponse::suggestedStartDate))
                .limit(5)
                .toList();
    }

    @Transactional(readOnly = true)
    public SessionConflictCheckResponse checkConflicts(SessionConflictCheckRequest request) {
        return checkConflicts(request, null);
    }

    @Transactional(readOnly = true)
    public SessionConflictCheckResponse checkConflicts(SessionConflictCheckRequest request, Long ignoredSessionId) {
        List<SessionConflictItem> conflicts = new ArrayList<>();

        if (request.startDate() == null || request.endDate() == null || !request.startDate().isBefore(request.endDate())) {
            conflicts.add(new SessionConflictItem(
                    "INVALID_DATE_RANGE",
                    ConflictSeverity.BLOCKING,
                    "Session start date must be before end date.",
                    null,
                    null
            ));
            return response(conflicts);
        }

        Formation formation = formationService.getFormation(request.formationId());
        if (!Boolean.TRUE.equals(formation.getActive())) {
            conflicts.add(new SessionConflictItem(
                    "FORMATION_INACTIVE",
                    ConflictSeverity.WARNING,
                    "The selected formation is inactive.",
                    null,
                    formation.getTitle()
            ));
        }

        trainerTimeConflicts(request, ignoredSessionId).forEach(session -> conflicts.add(new SessionConflictItem(
                "TRAINER_TIME_CONFLICT",
                ConflictSeverity.BLOCKING,
                "Trainer already has another active session during this time.",
                session.getId(),
                session.getTitle()
        )));

        List<TrainerAvailability> trainerAvailability =
                availabilityRepository.findByTrainerIdOrderByDayOfWeekAscStartTimeAsc(request.trainerId());
        if (!availabilityCovers(trainerAvailability, request.startDate(), request.endDate())) {
            conflicts.add(new SessionConflictItem(
                    "TRAINER_UNAVAILABLE",
                    ConflictSeverity.WARNING,
                    availabilityWarningMessage(trainerAvailability, request.startDate(), request.endDate()),
                    null,
                    null
            ));
        }

        if (Boolean.FALSE.equals(request.online()) && request.location() != null && !request.location().isBlank()) {
            locationConflicts(request, ignoredSessionId).forEach(session -> conflicts.add(new SessionConflictItem(
                    "LOCATION_CONFLICT",
                    ConflictSeverity.BLOCKING,
                    "Another onsite session already uses this location during this time.",
                    session.getId(),
                    session.getTitle()
            )));
        }

        return response(conflicts);
    }

    @Transactional(readOnly = true)
    public List<TrainerWorkloadResponse> workload(LocalDate from, LocalDate to) {
        LocalDate start = from == null ? LocalDate.now().withDayOfMonth(1) : from;
        LocalDate end = to == null ? start.plusMonths(1).minusDays(1) : to;
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(LocalTime.MAX);

        return trainerProfileRepository.findAll().stream()
                .map(trainer -> workloadForTrainer(trainer, startDateTime, endDateTime))
                .sorted(Comparator.comparing(TrainerWorkloadResponse::totalHours).reversed())
                .toList();
    }

    public boolean hasBlockingConflicts(SessionConflictCheckResponse response) {
        return response.conflicts().stream().anyMatch(item -> item.severity() == ConflictSeverity.BLOCKING);
    }

    private Optional<SessionPlanningSuggestionResponse> bestSuggestion(
            List<SessionPlanningSuggestionResponse> suggestions
    ) {
        return suggestions.stream()
                .max(Comparator
                        .comparingInt(SessionPlanningSuggestionResponse::score)
                        .thenComparing(SessionPlanningSuggestionResponse::availabilityMatch)
                        .thenComparing(SessionPlanningSuggestionResponse::conflictFree)
                        .thenComparing(SessionPlanningSuggestionResponse::suggestedStartDate, Comparator.reverseOrder()));
    }

    private List<SessionPlanningSuggestionResponse> candidateSuggestions(
            Formation formation,
            TrainerProfile trainer,
            SessionPlanningSuggestionRequest request
    ) {
        List<TrainerAvailability> availabilities = availabilityRepository.findByTrainerIdOrderByDayOfWeekAscStartTimeAsc(trainer.getId());
        List<SessionPlanningSuggestionResponse> suggestions = new ArrayList<>();

        for (LocalDate date = request.preferredStartDate(); !date.isAfter(request.preferredEndDate()); date = date.plusDays(1)) {
            LocalDate candidateDate = date;
            availabilities.stream()
                    .filter(availability -> availability.getDayOfWeek() == candidateDate.getDayOfWeek())
                    .forEach(availability -> suggestions.add(toSuggestion(formation, trainer, request, candidateDate, availability)));
        }

        if (suggestions.isEmpty()) {
            LocalDate fallbackDate = request.preferredStartDate();
            LocalDateTime start = fallbackDate.atTime(9, 0);
            LocalDateTime end = start.plusHours(request.durationHours());
            suggestions.add(toSuggestion(formation, trainer, request, start, end, false, List.of("No availability slot found in preferred range.")));
        }

        return suggestions;
    }

    private SessionPlanningSuggestionResponse toSuggestion(
            Formation formation,
            TrainerProfile trainer,
            SessionPlanningSuggestionRequest request,
            LocalDate date,
            TrainerAvailability availability
    ) {
        LocalDateTime start = date.atTime(availability.getStartTime());
        LocalDateTime desiredEnd = start.plusHours(request.durationHours());
        LocalDateTime end = desiredEnd.toLocalTime().isAfter(availability.getEndTime())
                ? date.atTime(availability.getEndTime())
                : desiredEnd;
        List<String> warnings = desiredEnd.isAfter(end)
                ? List.of("Suggested duration may require multiple sessions.")
                : List.of();
        return toSuggestion(formation, trainer, request, start, end, desiredEnd.equals(end), warnings);
    }

    private SessionPlanningSuggestionResponse toSuggestion(
            Formation formation,
            TrainerProfile trainer,
            SessionPlanningSuggestionRequest request,
            LocalDateTime start,
            LocalDateTime end,
            boolean availabilityMatch,
            List<String> initialWarnings
    ) {
        int score = 0;
        List<String> reasons = new ArrayList<>();
        List<String> warnings = new ArrayList<>(initialWarnings);
        int expertiseMatch = expertiseMatchPercentage(formation, trainer);
        WorkloadLevel workloadLevel = workloadLevelForMonth(trainer.getId(), start.toLocalDate());
        boolean conflictFree = trainerTimeConflicts(new SessionConflictCheckRequest(
                formation.getId(),
                trainer.getId(),
                start,
                end,
                request.online(),
                null
        ), null).isEmpty();

        if (expertiseMatch > 0) {
            score += 30;
            reasons.add("Expertise matches " + expertiseMatch + "% of required formation skills.");
        } else {
            warnings.add("Trainer expertise does not match required formation skills.");
        }
        if (availabilityMatch) {
            score += 20;
            reasons.add("Trainer is available during the suggested time.");
        } else {
            warnings.add("Trainer availability does not fully cover the suggested time.");
        }
        if (conflictFree) {
            score += 20;
            reasons.add("No planning conflict detected.");
        } else {
            score -= 40;
            warnings.add("Trainer has a planning conflict in this slot.");
        }
        if (workloadLevel == WorkloadLevel.LOW || workloadLevel == WorkloadLevel.NORMAL) {
            score += 15;
            reasons.add("Trainer workload is " + workloadLevel.name().toLowerCase() + " this week.");
        } else if (workloadLevel == WorkloadLevel.HIGH) {
            score -= 20;
            warnings.add("Trainer has high workload this month.");
        } else {
            score -= 20;
            warnings.add("Trainer is overloaded this month.");
        }
        if (trainer.getAverageRating() != null && trainer.getAverageRating() >= 4.0) {
            score += 10;
            reasons.add("Trainer average rating is at least 4.");
        }

        return new SessionPlanningSuggestionResponse(
                trainer.getId(),
                fullName(trainer),
                trainer.getUser().getEmail(),
                start,
                end,
                score,
                workloadLevel,
                expertiseMatch,
                availabilityMatch,
                conflictFree,
                reasons,
                warnings
        );
    }

    private TrainerWorkloadResponse workloadForTrainer(TrainerProfile trainer, LocalDateTime from, LocalDateTime to) {
        List<TrainingSession> sessions = sessionRepository.findByTrainerIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                trainer.getId(),
                to,
                from
        );
        List<TrainerWorkloadSessionItem> items = sessions.stream()
                .map(session -> new TrainerWorkloadSessionItem(
                        session.getId(),
                        session.getTitle(),
                        session.getFormation().getTitle(),
                        session.getStartDate(),
                        session.getEndDate(),
                        session.getStatus(),
                        durationHours(session.getStartDate(), session.getEndDate())
                ))
                .sorted(Comparator.comparing(TrainerWorkloadSessionItem::startDate))
                .toList();
        long totalHours = items.stream().mapToLong(TrainerWorkloadSessionItem::durationHours).sum();
        WorkloadLevel level = workloadLevel(totalHours);
        int completed = (int) sessions.stream().filter(session -> session.getStatus() == SessionStatus.COMPLETED).count();
        int upcoming = (int) sessions.stream()
                .filter(session -> session.getStartDate().isAfter(LocalDateTime.now()))
                .filter(session -> session.getStatus() != SessionStatus.CANCELLED)
                .count();

        return new TrainerWorkloadResponse(
                trainer.getId(),
                fullName(trainer),
                trainer.getUser().getEmail(),
                sessions.size(),
                totalHours,
                completed,
                upcoming,
                level,
                recommendation(level),
                items
        );
    }

    public WorkloadLevel workloadLevel(long hours) {
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

    private WorkloadLevel workloadLevelForMonth(Long trainerId, LocalDate date) {
        LocalDate firstDay = date.withDayOfMonth(1);
        LocalDate lastDay = firstDay.plusMonths(1).minusDays(1);
        List<TrainingSession> sessions = sessionRepository.findByTrainerIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                trainerId,
                lastDay.atTime(LocalTime.MAX),
                firstDay.atStartOfDay()
        );
        long hours = sessions.stream()
                .filter(session -> session.getStatus() != SessionStatus.CANCELLED)
                .mapToLong(session -> durationHours(session.getStartDate(), session.getEndDate()))
                .sum();
        return workloadLevel(hours);
    }

    private List<TrainingSession> trainerTimeConflicts(SessionConflictCheckRequest request, Long ignoredSessionId) {
        return sessionRepository.findByTrainerIdAndStartDateLessThanAndEndDateGreaterThanAndStatusIn(
                        request.trainerId(),
                        request.endDate(),
                        request.startDate(),
                        ACTIVE_SESSION_STATUSES
                ).stream()
                .filter(session -> ignoredSessionId == null || !session.getId().equals(ignoredSessionId))
                .toList();
    }

    private List<TrainingSession> locationConflicts(SessionConflictCheckRequest request, Long ignoredSessionId) {
        return sessionRepository.findByOnlineFalseAndLocationIgnoreCaseAndStartDateLessThanAndEndDateGreaterThanAndStatusIn(
                        request.location(),
                        request.endDate(),
                        request.startDate(),
                        ACTIVE_SESSION_STATUSES
                ).stream()
                .filter(session -> ignoredSessionId == null || !session.getId().equals(ignoredSessionId))
                .toList();
    }

    private boolean availabilityCovers(
            List<TrainerAvailability> availabilities,
            LocalDateTime start,
            LocalDateTime end
    ) {
        return uncoveredDates(availabilities, start, end).isEmpty();
    }

    private String availabilityWarningMessage(
            List<TrainerAvailability> availabilities,
            LocalDateTime start,
            LocalDateTime end
    ) {
        String declared = availabilities.isEmpty()
                ? "No weekly availability has been published."
                : "Declared weekly availability: " + availabilities.stream()
                        .sorted(Comparator
                                .comparingInt((TrainerAvailability availability) -> availability.getDayOfWeek().getValue())
                                .thenComparing(TrainerAvailability::getStartTime))
                        .map(availability -> availability.getDayOfWeek() + " "
                                + formatTime(availability.getStartTime()) + "-" + formatTime(availability.getEndTime()))
                        .collect(Collectors.joining(", ")) + ".";

        String missingDays = uncoveredDates(availabilities, start, end).stream()
                .map(date -> date.getDayOfWeek() + " " + date)
                .collect(Collectors.joining(", "));
        return "Requested daily hours " + formatTime(start.toLocalTime()) + "-"
                + formatTime(end.toLocalTime()) + " are not covered on: " + missingDays + ". "
                + declared + " Choose covered hours or update the trainer's availability.";
    }

    private List<LocalDate> uncoveredDates(
            List<TrainerAvailability> availabilities,
            LocalDateTime start,
            LocalDateTime end
    ) {
        LocalTime dailyStart = start.toLocalTime();
        LocalTime dailyEnd = end.toLocalTime();
        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate date = start.toLocalDate(); !date.isAfter(end.toLocalDate()); date = date.plusDays(1)) {
            LocalDate requestedDate = date;
            boolean covered = availabilities.stream()
                    .filter(availability -> availability.getDayOfWeek() == requestedDate.getDayOfWeek())
                    .anyMatch(availability ->
                            !dailyStart.isBefore(availability.getStartTime())
                                    && !dailyEnd.isAfter(availability.getEndTime())
                    );
            if (!covered) {
                dates.add(date);
            }
        }
        return dates;
    }

    private String formatTime(LocalTime time) {
        return String.format("%02d:%02d", time.getHour(), time.getMinute());
    }

    private int expertiseMatchPercentage(Formation formation, TrainerProfile trainer) {
        Set<Skill> requiredSkills = formation.getRequiredSkills();
        if (requiredSkills == null || requiredSkills.isEmpty()) {
            return 100;
        }
        Set<Long> trainerSkillIds = trainer.getExpertise().stream()
                .map(Skill::getId)
                .collect(Collectors.toSet());
        long matches = requiredSkills.stream()
                .filter(skill -> trainerSkillIds.contains(skill.getId()))
                .count();
        return (int) Math.round(matches * 100.0 / requiredSkills.size());
    }

    private SessionConflictCheckResponse response(List<SessionConflictItem> conflicts) {
        boolean blocking = conflicts.stream().anyMatch(item -> item.severity() == ConflictSeverity.BLOCKING);
        boolean warnings = conflicts.stream().anyMatch(item -> item.severity() == ConflictSeverity.WARNING);
        return new SessionConflictCheckResponse(blocking, warnings, conflicts);
    }

    private String recommendation(WorkloadLevel level) {
        return switch (level) {
            case LOW -> "Trainer has availability for more sessions.";
            case NORMAL -> "Trainer workload is balanced.";
            case HIGH -> "Avoid assigning too many additional sessions.";
            case OVERLOADED -> "Trainer is overloaded. Reassign future sessions if possible.";
        };
    }

    private long durationHours(LocalDateTime start, LocalDateTime end) {
        return Math.max(1, Duration.between(start, end).toHours());
    }

    private String fullName(TrainerProfile trainer) {
        return trainer.getUser().getFirstName() + " " + trainer.getUser().getLastName();
    }
}
