package com.training.platform.attendance.service;

import com.training.platform.attendance.config.AttendanceProperties;
import com.training.platform.attendance.dto.ClassroomAttendanceEntryResponse;
import com.training.platform.attendance.dto.ClassroomAttendanceReportResponse;
import com.training.platform.attendance.dto.ClassroomContextResponse;
import com.training.platform.attendance.entity.ClassroomPresenceInterval;
import com.training.platform.attendance.repository.ClassroomPresenceIntervalRepository;
import com.training.platform.common.exception.BadRequestException;
import com.training.platform.common.exception.ResourceNotFoundException;
import com.training.platform.enrollment.entity.Enrollment;
import com.training.platform.enrollment.entity.EnrollmentStatus;
import com.training.platform.enrollment.repository.EnrollmentRepository;
import com.training.platform.session.entity.SessionStatus;
import com.training.platform.session.entity.TrainingSession;
import com.training.platform.session.repository.TrainingSessionRepository;
import com.training.platform.session.service.TrainingSessionService;
import com.training.platform.user.entity.Role;
import com.training.platform.user.entity.User;
import com.training.platform.user.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VirtualAttendanceService {

    private final ClassroomPresenceIntervalRepository presenceRepository;
    private final TrainingSessionService sessionService;
    private final TrainingSessionRepository sessionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final AttendanceProperties properties;

    public VirtualAttendanceService(
            ClassroomPresenceIntervalRepository presenceRepository,
            TrainingSessionService sessionService,
            TrainingSessionRepository sessionRepository,
            EnrollmentRepository enrollmentRepository,
            UserRepository userRepository,
            AttendanceProperties properties
    ) {
        this.presenceRepository = presenceRepository;
        this.sessionService = sessionService;
        this.sessionRepository = sessionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.properties = properties;
    }

    @Transactional
    public ClassroomContextResponse getContext(Long sessionId, String email) {
        User user = getUser(email);
        TrainingSession session = getOnlineInProgressSession(sessionId);
        assertCanAccessClassroom(session, user);
        ensureClassroomRoomKey(session);
        return buildContext(session, user);
    }

    @Transactional
    public ClassroomContextResponse join(Long sessionId, String email) {
        User user = getUser(email);
        TrainingSession session = getOnlineInProgressSession(sessionId);
        assertCanAccessClassroom(session, user);
        ensureClassroomRoomKey(session);
        closeStaleIntervals(sessionId);
        Instant now = Instant.now();
        Optional<ClassroomPresenceInterval> open = presenceRepository
                .findFirstBySessionIdAndUserIdAndLeftAtIsNull(sessionId, user.getId());
        if (open.isPresent()) {
            ClassroomPresenceInterval interval = open.get();
            interval.setLastHeartbeatAt(now);
            presenceRepository.save(interval);
        } else {
            Enrollment enrollment = resolveEnrollment(session, user).orElse(null);
            ClassroomPresenceInterval interval = ClassroomPresenceInterval.builder()
                    .session(session)
                    .user(user)
                    .enrollment(enrollment)
                    .joinedAt(now)
                    .lastHeartbeatAt(now)
                    .build();
            presenceRepository.save(interval);
        }
        return buildContext(session, user);
    }

    @Transactional
    public void heartbeat(Long sessionId, String email) {
        User user = getUser(email);
        TrainingSession session = getOnlineInProgressSession(sessionId);
        assertCanAccessClassroom(session, user);
        closeStaleIntervals(sessionId);
        ClassroomPresenceInterval interval = presenceRepository
                .findFirstBySessionIdAndUserIdAndLeftAtIsNull(sessionId, user.getId())
                .orElseThrow(() -> new BadRequestException("No active classroom presence. Join the classroom first."));
        interval.setLastHeartbeatAt(Instant.now());
        presenceRepository.save(interval);
    }

    @Transactional
    public void leave(Long sessionId, String email) {
        User user = getUser(email);
        getOnlineInProgressSession(sessionId);
        assertCanAccessClassroom(sessionService.getSession(sessionId), user);
        presenceRepository.findFirstBySessionIdAndUserIdAndLeftAtIsNull(sessionId, user.getId())
                .ifPresent(interval -> {
                    Instant now = Instant.now();
                    interval.setLeftAt(now);
                    interval.setLastHeartbeatAt(now);
                    presenceRepository.save(interval);
                });
    }

    @Transactional(readOnly = true)
    public ClassroomAttendanceReportResponse getAttendanceReport(Long sessionId, String email) {
        User user = getUser(email);
        TrainingSession session = sessionService.getSession(sessionId);
        assertCanViewAttendanceReport(session, user);
        return buildReport(session);
    }

    @Transactional
    public void completeSmart(Long sessionId, String email) {
        User user = getUser(email);
        TrainingSession session = sessionService.getSession(sessionId);
        assertCanCompleteSmart(session, user);
        if (!Boolean.TRUE.equals(session.getOnline())) {
            throw new BadRequestException("Smart completion is only available for online sessions");
        }
        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new BadRequestException("Session must be in progress to complete with smart attendance");
        }
        closeStaleIntervals(sessionId);
        ClassroomAttendanceReportResponse report = buildReport(session);
        if (report.trainerActiveSeconds() <= 0) {
            throw new BadRequestException("Cannot complete: trainer was not present in the classroom");
        }
        List<Long> qualifiedIds = new ArrayList<>();
        for (ClassroomAttendanceEntryResponse entry : report.learners()) {
            enrollmentRepository.findById(entry.enrollmentId()).ifPresent(enrollment -> {
                enrollment.setVirtualAttendancePercentage(entry.attendancePercentage());
                enrollment.setVirtualAttendanceQualified(entry.qualified());
                enrollmentRepository.save(enrollment);
                if (entry.qualified() && enrollment.getStatus() == EnrollmentStatus.CONFIRMED) {
                    qualifiedIds.add(enrollment.getId());
                }
            });
        }
        sessionService.complete(sessionId, qualifiedIds);
    }

    @Transactional
    public void deleteBySessionId(Long sessionId) {
        presenceRepository.deleteBySessionId(sessionId);
    }

    private ClassroomContextResponse buildContext(TrainingSession session, User user) {
        boolean moderator = user.getRole() == Role.ADMIN
                || session.getTrainer().getUser().getId().equals(user.getId());
        return new ClassroomContextResponse(
                session.getId(),
                session.getTitle(),
                properties.jitsiDomain(),
                roomName(session),
                user.getFirstName() + " " + user.getLastName(),
                moderator,
                properties.heartbeatIntervalSec(),
                properties.thresholdPercent()
        );
    }

    private ClassroomAttendanceReportResponse buildReport(TrainingSession session) {
        closeStaleIntervals(session.getId());
        Instant now = Instant.now();
        List<ClassroomPresenceInterval> intervals = presenceRepository.findBySessionIdOrderByJoinedAtAsc(session.getId());
        Long trainerUserId = session.getTrainer().getUser().getId();
        List<TimeRange> trainerRanges = mergeRanges(toEffectiveRanges(
                intervals.stream().filter(i -> i.getUser().getId().equals(trainerUserId)).toList(),
                now
        ));
        long trainerActiveSeconds = totalSeconds(trainerRanges);
        List<Enrollment> enrollments = enrollmentRepository.findBySessionIdOrderByEnrolledAtDesc(session.getId());
        List<ClassroomAttendanceEntryResponse> learners = enrollments.stream()
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.CONFIRMED
                        || enrollment.getStatus() == EnrollmentStatus.COMPLETED)
                .map(enrollment -> {
                    Long learnerUserId = enrollment.getLearner().getUser().getId();
                    List<TimeRange> learnerRanges = mergeRanges(toEffectiveRanges(
                            intervals.stream().filter(i -> i.getUser().getId().equals(learnerUserId)).toList(),
                            now
                    ));
                    long trackedSeconds = totalSeconds(intersectRanges(learnerRanges, trainerRanges));
                    int percentage = trainerActiveSeconds == 0
                            ? 0
                            : (int) Math.min(100, Math.round((trackedSeconds * 100.0) / trainerActiveSeconds));
                    boolean qualified = percentage >= properties.thresholdPercent();
                    boolean connected = intervals.stream()
                            .filter(i -> i.getUser().getId().equals(learnerUserId))
                            .anyMatch(i -> isConnected(i, now));
                    String learnerFullName = enrollment.getLearner().getUser().getFirstName()
                            + " "
                            + enrollment.getLearner().getUser().getLastName();
                    return new ClassroomAttendanceEntryResponse(
                            enrollment.getId(),
                            enrollment.getLearner().getId(),
                            learnerFullName,
                            enrollment.getStatus(),
                            connected,
                            trackedSeconds,
                            trainerActiveSeconds,
                            percentage,
                            qualified
                    );
                })
                .toList();
        return new ClassroomAttendanceReportResponse(
                session.getId(),
                trainerActiveSeconds,
                properties.thresholdPercent(),
                learners
        );
    }

    private void ensureClassroomRoomKey(TrainingSession session) {
        if (Boolean.TRUE.equals(session.getOnline()) && session.getClassroomRoomKey() == null) {
            session.setClassroomRoomKey(UUID.randomUUID().toString());
            sessionRepository.save(session);
        }
    }

    private TrainingSession getOnlineInProgressSession(Long sessionId) {
        TrainingSession session = sessionService.getSession(sessionId);
        if (!Boolean.TRUE.equals(session.getOnline())) {
            throw new BadRequestException("This session is not an online classroom session");
        }
        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new BadRequestException("Classroom is only available while the session is in progress");
        }
        return session;
    }

    private void assertCanAccessClassroom(TrainingSession session, User user) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        if (user.getRole() == Role.TRAINER && session.getTrainer().getUser().getId().equals(user.getId())) {
            return;
        }
        if (user.getRole() == Role.LEARNER && hasConfirmedEnrollment(session.getId(), user.getId())) {
            return;
        }
        throw new AccessDeniedException("You cannot access this classroom");
    }

    private void assertCanViewAttendanceReport(TrainingSession session, User user) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        if (user.getRole() == Role.TRAINER && session.getTrainer().getUser().getId().equals(user.getId())) {
            return;
        }
        throw new AccessDeniedException("You cannot view classroom attendance for this session");
    }

    private void assertCanCompleteSmart(TrainingSession session, User user) {
        assertCanViewAttendanceReport(session, user);
    }

    private boolean hasConfirmedEnrollment(Long sessionId, Long userId) {
        return enrollmentRepository.findBySessionIdOrderByEnrolledAtDesc(sessionId).stream()
                .anyMatch(enrollment ->
                        enrollment.getLearner().getUser().getId().equals(userId)
                                && enrollment.getStatus() == EnrollmentStatus.CONFIRMED
                );
    }

    private Optional<Enrollment> resolveEnrollment(TrainingSession session, User user) {
        if (user.getRole() != Role.LEARNER) {
            return Optional.empty();
        }
        return enrollmentRepository.findBySessionIdOrderByEnrolledAtDesc(session.getId()).stream()
                .filter(enrollment ->
                        enrollment.getLearner().getUser().getId().equals(user.getId())
                                && enrollment.getStatus() == EnrollmentStatus.CONFIRMED
                )
                .findFirst();
    }

    private void closeStaleIntervals(Long sessionId) {
        Instant now = Instant.now();
        presenceRepository.findBySessionIdOrderByJoinedAtAsc(sessionId).stream()
                .filter(interval -> interval.getLeftAt() == null && !isConnected(interval, now))
                .forEach(interval -> {
                    Instant effectiveEnd = interval.getLastHeartbeatAt().plusSeconds(properties.staleAfterSec());
                    interval.setLeftAt(effectiveEnd.isAfter(now) ? now : effectiveEnd);
                    presenceRepository.save(interval);
                });
    }

    private boolean isConnected(ClassroomPresenceInterval interval, Instant now) {
        if (interval.getLeftAt() != null) {
            return false;
        }
        return !interval.getLastHeartbeatAt().plusSeconds(properties.staleAfterSec()).isBefore(now);
    }

    private List<TimeRange> toEffectiveRanges(List<ClassroomPresenceInterval> intervals, Instant now) {
        return intervals.stream()
                .map(interval -> {
                    Instant start = interval.getJoinedAt();
                    Instant end = interval.getLeftAt() != null
                            ? interval.getLeftAt()
                            : interval.getLastHeartbeatAt().plusSeconds(properties.staleAfterSec());
                    if (end.isAfter(now)) {
                        end = now;
                    }
                    if (!end.isAfter(start)) {
                        return null;
                    }
                    return new TimeRange(start, end);
                })
                .filter(range -> range != null)
                .sorted(Comparator.comparing(TimeRange::start))
                .toList();
    }

    private List<TimeRange> mergeRanges(List<TimeRange> ranges) {
        if (ranges.isEmpty()) {
            return List.of();
        }
        List<TimeRange> merged = new ArrayList<>();
        TimeRange current = ranges.getFirst();
        for (int i = 1; i < ranges.size(); i++) {
            TimeRange next = ranges.get(i);
            if (!next.start().isAfter(current.end())) {
                Instant end = next.end().isAfter(current.end()) ? next.end() : current.end();
                current = new TimeRange(current.start(), end);
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        return merged;
    }

    private List<TimeRange> intersectRanges(List<TimeRange> left, List<TimeRange> right) {
        List<TimeRange> intersections = new ArrayList<>();
        for (TimeRange a : left) {
            for (TimeRange b : right) {
                Instant start = a.start().isAfter(b.start()) ? a.start() : b.start();
                Instant end = a.end().isBefore(b.end()) ? a.end() : b.end();
                if (end.isAfter(start)) {
                    intersections.add(new TimeRange(start, end));
                }
            }
        }
        return mergeRanges(intersections);
    }

    private long totalSeconds(List<TimeRange> ranges) {
        return ranges.stream()
                .mapToLong(range -> range.end().getEpochSecond() - range.start().getEpochSecond())
                .sum();
    }

    private String roomName(TrainingSession session) {
        return "training-platform-" + session.getClassroomRoomKey();
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private record TimeRange(Instant start, Instant end) {
    }
}
