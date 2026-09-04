package com.training.platform.session.service;

import com.training.platform.formation.dto.FormationProgressSnapshot;
import com.training.platform.formation.service.FormationProgressService;
import com.training.platform.catalogue.formation.entity.Formation;
import com.training.platform.catalogue.formation.service.FormationService;
import com.training.platform.common.exception.BadRequestException;
import com.training.platform.common.exception.ResourceNotFoundException;
import com.training.platform.enrollment.entity.Enrollment;
import com.training.platform.enrollment.entity.EnrollmentStatus;
import com.training.platform.enrollment.repository.EnrollmentRepository;
import com.training.platform.learner.entity.LearnerProfile;
import com.training.platform.notification.entity.NotificationType;
import com.training.platform.notification.service.NotificationService;
import com.training.platform.skill.entity.Skill;
import com.training.platform.planning.dto.ConflictSeverity;
import com.training.platform.planning.dto.SessionConflictCheckRequest;
import com.training.platform.planning.dto.SessionConflictCheckResponse;
import com.training.platform.planning.service.SessionPlanningService;
import com.training.platform.session.dto.TrainingSessionRequest;
import com.training.platform.session.dto.TrainingSessionResponse;
import com.training.platform.session.dto.TrainingSessionUpdateRequest;
import com.training.platform.session.entity.SessionStatus;
import com.training.platform.session.entity.TrainingSession;
import com.training.platform.session.repository.TrainingSessionRepository;
import com.training.platform.trainer.entity.TrainerProfile;
import com.training.platform.trainer.service.TrainerService;
import com.training.platform.user.entity.User;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrainingSessionService {

    private final TrainingSessionRepository sessionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final FormationService formationService;
    private final TrainerService trainerService;
    private final TrainingSessionMapper sessionMapper;
    private final SessionPlanningService planningService;
    private final NotificationService notificationService;
    private final FormationProgressService formationProgressService;

    public TrainingSessionService(
            TrainingSessionRepository sessionRepository,
            EnrollmentRepository enrollmentRepository,
            FormationService formationService,
            TrainerService trainerService,
            TrainingSessionMapper sessionMapper,
            SessionPlanningService planningService,
            NotificationService notificationService,
            FormationProgressService formationProgressService
    ) {
        this.sessionRepository = sessionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.formationService = formationService;
        this.trainerService = trainerService;
        this.sessionMapper = sessionMapper;
        this.planningService = planningService;
        this.notificationService = notificationService;
        this.formationProgressService = formationProgressService;
    }

    @Transactional
    public TrainingSessionResponse create(TrainingSessionRequest request) {
        validateDates(request.startDate(), request.endDate());
        validateLocation(request.online(), request.location());
        Formation formation = formationService.getFormation(request.formationId());
        TrainerProfile trainer = trainerService.getProfile(request.trainerId());
        SessionStatus targetStatus = request.status() == null ? SessionStatus.PLANNED : request.status();
        if (targetStatus != SessionStatus.CANCELLED) {
            ensureFormationSessionCapacity(formation, null);
        }
        rejectBlockingConflicts(new SessionConflictCheckRequest(
                request.formationId(),
                request.trainerId(),
                request.startDate(),
                request.endDate(),
                request.online(),
                request.location()
        ), null);

        TrainingSession session = TrainingSession.builder()
                .formation(formation)
                .trainer(trainer)
                .title(request.title())
                .description(request.description())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .capacity(request.capacity())
                .location(request.location())
                .online(request.online() != null && request.online())
                .meetingUrl(request.meetingUrl())
                .status(request.status() == null ? SessionStatus.PLANNED : request.status())
                .build();

        TrainingSession saved = sessionRepository.save(session);
        sendAutomaticRemindersIfDue(saved);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<TrainingSessionResponse> findAll(
            String keyword,
            Long formationId,
            Long trainerId,
            SessionStatus status,
            Boolean online,
            Pageable pageable
    ) {
        return sessionRepository.findAll(
                        TrainingSessionSpecifications.withFilters(keyword, formationId, trainerId, status, online),
                        pageable
                )
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TrainingSessionResponse findById(Long id) {
        return toResponse(getSession(id));
    }

    @Transactional(readOnly = true)
    public List<TrainingSessionResponse> findByFormation(Long formationId) {
        formationService.getFormation(formationId);
        return sessionRepository.findByFormationId(formationId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TrainingSessionResponse> findAssignedToTrainer(String email) {
        return sessionRepository.findByTrainerUserEmail(email).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TrainingSessionResponse update(Long id, TrainingSessionUpdateRequest request) {
        validateDates(request.startDate(), request.endDate());
        validateLocation(request.online(), request.location());
        TrainingSession session = getSession(id);
        Formation formation = formationService.getFormation(request.formationId());
        TrainerProfile trainer = trainerService.getProfile(request.trainerId());
        rejectBlockingConflicts(new SessionConflictCheckRequest(
                request.formationId(),
                request.trainerId(),
                request.startDate(),
                request.endDate(),
                request.online(),
                request.location()
        ), id);

        SessionStatus targetStatus = request.status() == null ? session.getStatus() : request.status();
        if (targetStatus != SessionStatus.CANCELLED) {
            ensureFormationSessionCapacity(formation, session.getId());
        }

        LocalDateTime previousStart = session.getStartDate();
        LocalDateTime previousEnd = session.getEndDate();
        Boolean previousOnline = session.getOnline();
        String previousLocation = session.getLocation();
        String previousMeetingUrl = session.getMeetingUrl();
        SessionStatus previousStatus = session.getStatus();

        session.setFormation(formation);
        session.setTrainer(trainer);
        session.setTitle(request.title());
        session.setDescription(request.description());
        session.setStartDate(request.startDate());
        session.setEndDate(request.endDate());
        session.setCapacity(request.capacity());
        session.setLocation(request.location());
        session.setOnline(request.online() != null && request.online());
        session.setMeetingUrl(request.meetingUrl());
        session.setStatus(request.status() == null ? session.getStatus() : request.status());

        TrainingSession saved = sessionRepository.save(session);
        if (saved.getStatus() == SessionStatus.CANCELLED && previousStatus != SessionStatus.CANCELLED) {
            notifySessionEvent(
                    saved,
                    NotificationType.SESSION_CANCELLED,
                    "Session cancelled",
                    "The session " + saved.getTitle() + " was cancelled.",
                    true
            );
        } else if (saved.getStatus() != SessionStatus.CANCELLED
                && saved.getStatus() != SessionStatus.COMPLETED
                && scheduleOrPlaceChanged(previousStart, previousEnd, previousOnline, previousLocation, previousMeetingUrl, saved)) {
            notifySessionEvent(
                    saved,
                    NotificationType.SESSION_RESCHEDULED,
                    "Session rescheduled",
                    "The session " + saved.getTitle() + " was updated. New time: "
                            + saved.getStartDate() + " → " + saved.getEndDate() + ". " + placeLabel(saved),
                    true
            );
            saved.setReminderSentAt(null);
            saved.setHourReminderSentAt(null);
            sessionRepository.save(saved);
            sendAutomaticRemindersIfDue(saved);
        }
        return toResponse(saved);
    }

    @Transactional
    public TrainingSessionResponse updateStatus(Long id, SessionStatus status) {
        if (status == SessionStatus.IN_PROGRESS) {
            return start(id);
        }
        if (status == SessionStatus.CANCELLED) {
            return cancel(id);
        }
        TrainingSession session = getSession(id);
        session.setStatus(status);
        return toResponse(sessionRepository.save(session));
    }

    @Transactional
    public TrainingSessionResponse start(Long id) {
        TrainingSession session = getSession(id);
        if (session.getStatus() == SessionStatus.IN_PROGRESS) {
            throw new BadRequestException("This session is already in progress");
        }
        if (session.getStatus() == SessionStatus.COMPLETED || session.getStatus() == SessionStatus.CANCELLED) {
            throw new BadRequestException("This session cannot be started");
        }
        session.setStatus(SessionStatus.IN_PROGRESS);
        if (Boolean.TRUE.equals(session.getOnline()) && session.getClassroomRoomKey() == null) {
            session.setClassroomRoomKey(UUID.randomUUID().toString());
        }
        TrainingSession saved = sessionRepository.save(session);
        notifySessionEvent(
                saved,
                NotificationType.SESSION_STARTED,
                "Session started",
                "The session " + saved.getTitle() + " has started. " + placeLabel(saved),
                false
        );
        return toResponse(saved);
    }

    @Transactional
    public TrainingSessionResponse cancel(Long id) {
        TrainingSession session = getSession(id);
        if (session.getStatus() == SessionStatus.CANCELLED) {
            throw new BadRequestException("This session is already cancelled");
        }
        if (session.getStatus() == SessionStatus.COMPLETED) {
            throw new BadRequestException("A completed session cannot be cancelled");
        }
        session.setStatus(SessionStatus.CANCELLED);
        TrainingSession saved = sessionRepository.save(session);
        notifySessionEvent(
                saved,
                NotificationType.SESSION_CANCELLED,
                "Session cancelled",
                "The session " + saved.getTitle() + " was cancelled.",
                true
        );
        return toResponse(saved);
    }

    @Transactional
    public TrainingSessionResponse remind(Long id) {
        TrainingSession session = getSession(id);
        sendReminder(session, session.getStartDate().isBefore(LocalDateTime.now().plusHours(1)));
        return toResponse(session);
    }

    @Transactional
    public int sendDueReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<SessionStatus> upcoming = List.of(SessionStatus.PLANNED, SessionStatus.OPEN);
        List<TrainingSession> dayReminders = sessionRepository.findByStatusInAndStartDateBetweenAndReminderSentAtIsNull(
                upcoming,
                now.plusHours(1),
                now.plusDays(7)
        );
        dayReminders.forEach(session -> sendReminder(session, false));
        List<TrainingSession> hourReminders = sessionRepository.findByStatusInAndStartDateBetweenAndHourReminderSentAtIsNull(
                upcoming,
                now,
                now.plusHours(1)
        );
        hourReminders.forEach(session -> sendReminder(session, true));
        return dayReminders.size() + hourReminders.size();
    }

    @Transactional
    public TrainingSessionResponse complete(Long id, List<Long> presentEnrollmentIds) {
        TrainingSession session = getSession(id);
        if (session.getStatus() == SessionStatus.COMPLETED) {
            throw new BadRequestException("This session is already completed");
        }
        if (session.getStatus() == SessionStatus.CANCELLED) {
            throw new BadRequestException("A cancelled session cannot be completed");
        }

        Set<Long> presentIds = presentEnrollmentIds == null ? Set.of() : new HashSet<>(presentEnrollmentIds);
        List<Enrollment> enrollments = enrollmentRepository.findBySessionIdOrderByEnrolledAtDesc(id);
        Set<Skill> formationSkills = session.getFormation().getRequiredSkills() == null
                ? Set.of()
                : session.getFormation().getRequiredSkills();

        for (Enrollment enrollment : enrollments) {
            if (enrollment.getStatus() != EnrollmentStatus.CONFIRMED || !presentIds.contains(enrollment.getId())) {
                continue;
            }
            enrollment.setStatus(EnrollmentStatus.COMPLETED);
            enrollmentRepository.save(enrollment);
            notifyLearnerSessionProgress(enrollment, session, formationSkills);
        }

        session.setStatus(SessionStatus.COMPLETED);
        TrainingSession saved = sessionRepository.save(session);
        notificationService.push(
                session.getTrainer().getUser(),
                NotificationType.SESSION_COMPLETED,
                "Session completed",
                "You completed " + session.getTitle() + ".",
                "/trainer/my-sessions"
        );
        return toResponse(saved);
    }

    private void notifyLearnerSessionProgress(Enrollment enrollment, TrainingSession session, Set<Skill> formationSkills) {
        FormationProgressSnapshot progress = formationProgressService.snapshot(
                enrollment.getLearner().getId(),
                session.getFormation().getId()
        );
        if (progress.formationComplete()) {
            grantFormationSkills(enrollment.getLearner(), formationSkills);
            notificationService.push(
                    enrollment.getLearner().getUser(),
                    NotificationType.FORMATION_COMPLETED,
                    "Formation completed",
                    "You finished all sessions of " + session.getFormation().getTitle() + ". Your learning path has been updated.",
                    "/learner/learning-path"
            );
            return;
        }
        String progressLabel = progress.totalSessions() > 0
                ? progress.completedSessions() + " of " + progress.totalSessions() + " sessions done"
                : "session completed";
        notificationService.push(
                enrollment.getLearner().getUser(),
                NotificationType.SESSION_COMPLETED,
                "Session completed",
                "You completed " + session.getTitle() + ". " + progressLabel
                        + " (" + progress.progressPercentage() + "% of " + session.getFormation().getTitle() + ").",
                "/learner/my-enrollments"
        );
    }

    private void grantFormationSkills(LearnerProfile learner, Set<Skill> formationSkills) {
        if (formationSkills.isEmpty()) {
            return;
        }
        if (learner.getSkills() == null) {
            learner.setSkills(new HashSet<>());
        }
        learner.getSkills().addAll(formationSkills);
    }

    @Transactional
    public void delete(Long id) {
        sessionRepository.delete(getSession(id));
    }

    @Transactional(readOnly = true)
    public TrainingSession getSession(Long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Training session not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public boolean isAssignedTrainer(Long sessionId, String email) {
        return sessionRepository.findById(sessionId)
                .map(session -> session.getTrainer().getUser().getEmail().equals(email))
                .orElse(false);
    }

    private TrainingSessionResponse toResponse(TrainingSession session) {
        return sessionMapper.toResponse(session, confirmedCount(session.getId()));
    }

    private long confirmedCount(Long sessionId) {
        return enrollmentRepository.countBySessionIdAndStatusIn(
                sessionId,
                List.of(EnrollmentStatus.CONFIRMED, EnrollmentStatus.COMPLETED)
        );
    }

    private void validateDates(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate) {
        if (startDate != null && endDate != null && !startDate.isBefore(endDate)) {
            throw new BadRequestException("Session start date must be before end date");
        }
    }

    private void validateLocation(Boolean online, String location) {
        if (Boolean.FALSE.equals(online) && (location == null || location.isBlank())) {
            throw new BadRequestException("Location is required for onsite sessions");
        }
    }

    private void ensureFormationSessionCapacity(Formation formation, Long excludingSessionId) {
        long scheduled = sessionRepository.countByFormationIdAndStatusNot(formation.getId(), SessionStatus.CANCELLED);
        if (excludingSessionId != null) {
            TrainingSession excluded = sessionRepository.findById(excludingSessionId).orElse(null);
            if (excluded != null
                    && excluded.getFormation().getId().equals(formation.getId())
                    && excluded.getStatus() != SessionStatus.CANCELLED) {
                scheduled--;
            }
        }
        int allowed = formation.getSessionCount() != null ? formation.getSessionCount() : 1;
        if (scheduled >= allowed) {
            throw new BadRequestException(
                    "This formation allows " + allowed + " session(s). Cancel or delete an existing session before adding another."
            );
        }
    }

    private void rejectBlockingConflicts(SessionConflictCheckRequest request, Long ignoredSessionId) {
        SessionConflictCheckResponse response = planningService.checkConflicts(request, ignoredSessionId);
        if (planningService.hasBlockingConflicts(response)) {
            String message = response.conflicts().stream()
                    .filter(conflict -> conflict.severity() == ConflictSeverity.BLOCKING)
                    .map(conflict -> conflict.type() + ": " + conflict.message())
                    .findFirst()
                    .orElse("Blocking planning conflict detected");
            throw new BadRequestException(message);
        }
    }

    private void sendAutomaticRemindersIfDue(TrainingSession session) {
        if (session.getStatus() != SessionStatus.PLANNED && session.getStatus() != SessionStatus.OPEN) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = session.getStartDate();
        if (start == null || start.isBefore(now)) {
            return;
        }
        if (!start.isAfter(now.plusHours(1))) {
            sendReminder(session, true);
        } else if (!start.isAfter(now.plusDays(7))) {
            sendReminder(session, false);
        }
    }

    private void sendReminder(TrainingSession session, boolean hourReminder) {
        if (session.getStatus() == SessionStatus.COMPLETED || session.getStatus() == SessionStatus.CANCELLED) {
            return;
        }
        if (hourReminder && session.getHourReminderSentAt() != null) {
            return;
        }
        if (!hourReminder && session.getReminderSentAt() != null) {
            return;
        }
        String when = hourReminder
                ? "starts in less than an hour (" + session.getStartDate() + ")"
                : "starts at " + session.getStartDate();
        notifySessionEvent(
                session,
                NotificationType.SESSION_REMINDER,
                hourReminder ? "Session starting soon" : "Session reminder",
                "The session " + session.getTitle() + " " + when + ". " + placeLabel(session),
                true
        );
        Instant sentAt = Instant.now();
        if (hourReminder) {
            session.setHourReminderSentAt(sentAt);
            if (session.getReminderSentAt() == null) {
                session.setReminderSentAt(sentAt);
            }
        } else {
            session.setReminderSentAt(sentAt);
        }
        sessionRepository.save(session);
    }

    private void notifySessionEvent(
            TrainingSession session,
            NotificationType type,
            String title,
            String body,
            boolean includeTrainer
    ) {
        enrollmentRepository.findBySessionIdOrderByEnrolledAtDesc(session.getId()).stream()
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.CONFIRMED
                        || enrollment.getStatus() == EnrollmentStatus.WAITLISTED)
                .map(enrollment -> enrollment.getLearner().getUser())
                .forEach(user -> notificationService.push(user, type, title, body, "/learner/my-enrollments"));
        if (includeTrainer) {
            User trainer = session.getTrainer().getUser();
            notificationService.push(trainer, type, title, body, "/trainer/my-sessions");
        }
    }

    private boolean scheduleOrPlaceChanged(
            LocalDateTime previousStart,
            LocalDateTime previousEnd,
            Boolean previousOnline,
            String previousLocation,
            String previousMeetingUrl,
            TrainingSession saved
    ) {
        return !Objects.equals(previousStart, saved.getStartDate())
                || !Objects.equals(previousEnd, saved.getEndDate())
                || !Objects.equals(previousOnline, saved.getOnline())
                || !Objects.equals(blankToNull(previousLocation), blankToNull(saved.getLocation()))
                || !Objects.equals(blankToNull(previousMeetingUrl), blankToNull(saved.getMeetingUrl()));
    }

    private String placeLabel(TrainingSession session) {
        if (Boolean.TRUE.equals(session.getOnline())) {
            return session.getMeetingUrl() == null || session.getMeetingUrl().isBlank()
                    ? "Online"
                    : "Online: " + session.getMeetingUrl();
        }
        return session.getLocation() == null || session.getLocation().isBlank()
                ? "Onsite"
                : "Onsite: " + session.getLocation();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
