package com.training.platform.enrollment.service;

import com.training.platform.common.exception.BadRequestException;
import com.training.platform.common.exception.ResourceNotFoundException;
import com.training.platform.common.service.EmailService;
import com.training.platform.enrollment.dto.EnrollmentCancelResponse;
import com.training.platform.enrollment.dto.EnrollmentResponse;
import com.training.platform.enrollment.entity.Enrollment;
import com.training.platform.enrollment.entity.EnrollmentStatus;
import com.training.platform.enrollment.repository.EnrollmentRepository;
import com.training.platform.learner.entity.LearnerProfile;
import com.training.platform.learner.repository.LearnerProfileRepository;
import com.training.platform.formation.dto.FormationProgressSnapshot;
import com.training.platform.formation.service.FormationProgressService;
import com.training.platform.notification.entity.NotificationType;
import com.training.platform.notification.service.NotificationService;
import com.training.platform.session.entity.SessionStatus;
import com.training.platform.session.entity.TrainingSession;
import com.training.platform.session.service.TrainingSessionService;
import com.training.platform.skill.entity.Skill;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final LearnerProfileRepository learnerProfileRepository;
    private final TrainingSessionService trainingSessionService;
    private final EnrollmentMapper enrollmentMapper;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final FormationProgressService formationProgressService;

    public EnrollmentService(
            EnrollmentRepository enrollmentRepository,
            LearnerProfileRepository learnerProfileRepository,
            TrainingSessionService trainingSessionService,
            EnrollmentMapper enrollmentMapper,
            EmailService emailService,
            NotificationService notificationService,
            FormationProgressService formationProgressService
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.learnerProfileRepository = learnerProfileRepository;
        this.trainingSessionService = trainingSessionService;
        this.enrollmentMapper = enrollmentMapper;
        this.emailService = emailService;
        this.notificationService = notificationService;
        this.formationProgressService = formationProgressService;
    }

    @Transactional
    public EnrollmentResponse enroll(Long sessionId, String learnerEmail) {
        LearnerProfile learner = learnerProfileRepository.findByUserEmail(learnerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Learner profile not found for current user"));
        TrainingSession session = trainingSessionService.getSession(sessionId);
        assertSessionAcceptsEnrollment(session);
        EnrollmentStatus status = resolveEnrollmentStatus(session);

        return enrollmentRepository.findByLearnerIdAndSessionId(learner.getId(), session.getId())
                .map(existing -> reopen(existing, status))
                .orElseGet(() -> createEnrollment(learner, session, status));
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> findMine(String learnerEmail) {
        return enrollmentRepository.findByLearnerUserEmailOrderByEnrolledAtDesc(learnerEmail).stream()
                .map(enrollmentMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> findBySession(Long sessionId) {
        trainingSessionService.getSession(sessionId);
        return enrollmentRepository.findBySessionIdOrderByEnrolledAtDesc(sessionId).stream()
                .map(enrollmentMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EnrollmentResponse findById(Long id) {
        return enrollmentMapper.toResponse(getEnrollment(id));
    }

    @Transactional
    public EnrollmentResponse updateStatus(Long id, EnrollmentStatus status) {
        Enrollment enrollment = getEnrollment(id);
        EnrollmentStatus previous = enrollment.getStatus();
        enrollment.setStatus(status);
        Enrollment saved = enrollmentRepository.save(enrollment);
        if (status == EnrollmentStatus.CONFIRMED) {
            notifyLearner(
                    saved,
                    NotificationType.ENROLLMENT_APPROVED,
                    "Enrollment approved",
                    "An administrator approved your enrollment in " + saved.getSession().getTitle() + "."
            );
            if (previous == EnrollmentStatus.WAITLISTED) {
                String learnerName = saved.getLearner().getUser().getFirstName()
                        + " " + saved.getLearner().getUser().getLastName();
                notificationService.notifyAdmins(
                        NotificationType.ENROLLMENT_CONFIRMED,
                        "Enrollment confirmed",
                        learnerName + " was approved for “" + saved.getSession().getTitle() + "”.",
                        "/admin/enrollments"
                );
            }
        }
        if (status == EnrollmentStatus.COMPLETED) {
            applyFormationProgress(saved);
        }
        return enrollmentMapper.toResponse(saved);
    }

    @Transactional
    public EnrollmentCancelResponse cancel(Long id) {
        Enrollment enrollment = getEnrollment(id);
        boolean wasConfirmed = enrollment.getStatus() == EnrollmentStatus.CONFIRMED;
        String cancelledLearnerName = enrollment.getLearner().getUser().getFirstName()
                + " " + enrollment.getLearner().getUser().getLastName();
        String sessionTitle = enrollment.getSession().getTitle();

        enrollment.setStatus(EnrollmentStatus.CANCELLED);
        enrollmentRepository.save(enrollment);

        notificationService.notifyAdmins(
                NotificationType.ENROLLMENT_CANCELLED,
                "Enrollment cancelled",
                cancelledLearnerName + " cancelled enrollment for “" + sessionTitle + "”.",
                "/admin/enrollments"
        );

        if (!wasConfirmed) {
            return new EnrollmentCancelResponse(
                    enrollment.getId(),
                    false,
                    null,
                    null,
                    "Enrollment cancelled."
            );
        }

        return enrollmentRepository.findFirstBySessionIdAndStatusOrderByEnrolledAtAsc(
                        enrollment.getSession().getId(),
                        EnrollmentStatus.WAITLISTED
                )
                .map(waitlisted -> promote(waitlisted, enrollment.getId()))
                .orElseGet(() -> new EnrollmentCancelResponse(
                        enrollment.getId(),
                        false,
                        null,
                        null,
                        "Enrollment cancelled. No waitlisted learner was available for promotion."
                ));
    }

    @Transactional
    public void delete(Long id) {
        enrollmentRepository.delete(getEnrollment(id));
    }

    @Transactional(readOnly = true)
    public boolean isOwnerLearner(Long enrollmentId, String email) {
        return enrollmentRepository.findByIdAndLearnerUserEmail(enrollmentId, email).isPresent();
    }

    @Transactional(readOnly = true)
    public boolean isAssignedTrainerForEnrollment(Long enrollmentId, String email) {
        return enrollmentRepository.findById(enrollmentId)
                .map(enrollment -> enrollment.getSession().getTrainer().getUser().getEmail().equals(email))
                .orElse(false);
    }

    private Enrollment getEnrollment(Long id) {
        return enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id: " + id));
    }

    private EnrollmentResponse reopen(Enrollment existing, EnrollmentStatus status) {
        if (existing.getStatus() != EnrollmentStatus.CANCELLED) {
            throw new BadRequestException("Learner is already enrolled in this session");
        }
        existing.setStatus(status);
        existing.setEnrolledAt(LocalDateTime.now());
        Enrollment saved = enrollmentRepository.save(existing);
        notifyEnrollmentCreated(saved);
        return enrollmentMapper.toResponse(saved);
    }

    private EnrollmentResponse createEnrollment(
            LearnerProfile learner,
            TrainingSession session,
            EnrollmentStatus status
    ) {
        Enrollment enrollment = Enrollment.builder()
                .learner(learner)
                .session(session)
                .status(status)
                .enrolledAt(LocalDateTime.now())
                .build();
        Enrollment saved = enrollmentRepository.save(enrollment);
        notifyEnrollmentCreated(saved);
        return enrollmentMapper.toResponse(saved);
    }

    private void assertSessionAcceptsEnrollment(TrainingSession session) {
        if (session.getStatus() == SessionStatus.CANCELLED || session.getStatus() == SessionStatus.COMPLETED) {
            throw new BadRequestException("Cannot enroll in a cancelled or completed session");
        }
        if (session.getStatus() != SessionStatus.OPEN && session.getStatus() != SessionStatus.PLANNED) {
            throw new BadRequestException("Enrollment is allowed only for planned or open sessions");
        }
    }

    private EnrollmentStatus resolveEnrollmentStatus(TrainingSession session) {
        long confirmedCount = enrollmentRepository.countBySessionIdAndStatusIn(
                session.getId(),
                List.of(EnrollmentStatus.CONFIRMED, EnrollmentStatus.COMPLETED)
        );
        if (confirmedCount >= session.getCapacity()) {
            return EnrollmentStatus.WAITLISTED;
        }
        return EnrollmentStatus.CONFIRMED;
    }

    private void notifyEnrollmentCreated(Enrollment enrollment) {
        String learnerName = enrollment.getLearner().getUser().getFirstName()
                + " " + enrollment.getLearner().getUser().getLastName();
        String sessionTitle = enrollment.getSession().getTitle();

        if (enrollment.getStatus() == EnrollmentStatus.WAITLISTED) {
            notifyLearner(
                    enrollment,
                    NotificationType.ENROLLMENT_WAITLISTED,
                    "You are on the waitlist",
                    "The session " + sessionTitle + " is full. We will notify you if a seat opens."
            );
            notificationService.notifyAdmins(
                    NotificationType.ENROLLMENT_WAITLISTED,
                    "Waitlist request",
                    learnerName + " joined the waitlist for “" + sessionTitle + "”.",
                    "/admin/enrollments"
            );
            return;
        }
        if (enrollment.getStatus() == EnrollmentStatus.CONFIRMED) {
            notifyLearner(
                    enrollment,
                    NotificationType.ENROLLMENT_CONFIRMED,
                    "Enrollment confirmed",
                    "You are enrolled in " + sessionTitle + "."
            );
            notificationService.notifyAdmins(
                    NotificationType.ENROLLMENT_CONFIRMED,
                    "Enrollment confirmed",
                    learnerName + " enrolled in “" + sessionTitle + "”.",
                    "/admin/enrollments"
            );
        }
    }

    private void applyFormationProgress(Enrollment enrollment) {
        Set<Skill> formationSkills = enrollment.getSession().getFormation().getRequiredSkills() == null
                ? Set.of()
                : enrollment.getSession().getFormation().getRequiredSkills();
        FormationProgressSnapshot progress = formationProgressService.snapshot(
                enrollment.getLearner().getId(),
                enrollment.getSession().getFormation().getId()
        );
        if (progress.formationComplete()) {
            if (enrollment.getLearner().getSkills() == null) {
                enrollment.getLearner().setSkills(new java.util.HashSet<>());
            }
            enrollment.getLearner().getSkills().addAll(formationSkills);
            notifyLearner(
                    enrollment,
                    NotificationType.FORMATION_COMPLETED,
                    "Formation completed",
                    "You finished all sessions of " + enrollment.getSession().getFormation().getTitle() + "."
            );
            return;
        }
        notifyLearner(
                enrollment,
                NotificationType.SESSION_COMPLETED,
                "Session completed",
                "Session marked complete. " + progress.completedSessions() + " of " + progress.totalSessions()
                        + " sessions done for " + enrollment.getSession().getFormation().getTitle() + "."
        );
    }

    private void notifyLearner(Enrollment enrollment, NotificationType type, String title, String body) {
        notificationService.push(
                enrollment.getLearner().getUser(),
                type,
                title,
                body,
                "/learner/my-enrollments"
        );
    }

    private EnrollmentCancelResponse promote(Enrollment waitlisted, Long cancelledEnrollmentId) {
        waitlisted.setStatus(EnrollmentStatus.CONFIRMED);
        Enrollment promoted = enrollmentRepository.save(waitlisted);
        String learnerName = promoted.getLearner().getUser().getFirstName()
                + " " + promoted.getLearner().getUser().getLastName();
        String sessionTitle = promoted.getSession().getTitle();

        emailService.sendWaitlistPromotion(
                promoted.getLearner().getUser().getEmail(),
                learnerName,
                sessionTitle
        );
        notifyLearner(
                promoted,
                NotificationType.ENROLLMENT_APPROVED,
                "You got a seat",
                "A place opened in “" + sessionTitle + "”. Your waitlist spot is now confirmed — you are enrolled."
        );
        notificationService.notifyAdmins(
                NotificationType.ENROLLMENT_CONFIRMED,
                "Waitlist promoted",
                learnerName + " took the free place in “" + sessionTitle + "”.",
                "/admin/enrollments"
        );
        return new EnrollmentCancelResponse(
                cancelledEnrollmentId,
                true,
                promoted.getId(),
                learnerName,
                "Enrollment cancelled. " + learnerName + " was promoted from the waitlist."
        );
    }
}
