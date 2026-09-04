package com.training.platform.user.service;

import com.training.platform.auth.repository.PasswordResetTokenRepository;
import com.training.platform.chat.repository.ChatMessageRepository;
import com.training.platform.chat.repository.MessageReadReceiptRepository;
import com.training.platform.common.exception.BadRequestException;
import com.training.platform.common.exception.ResourceNotFoundException;
import com.training.platform.enrollment.repository.EnrollmentRepository;
import com.training.platform.learner.entity.LearnerProfile;
import com.training.platform.learner.repository.LearnerProfileRepository;
import com.training.platform.session.entity.TrainingSession;
import com.training.platform.session.repository.TrainingSessionRepository;
import com.training.platform.trainer.entity.TrainerProfile;
import com.training.platform.trainer.repository.TrainerAvailabilityRepository;
import com.training.platform.trainer.repository.TrainerProfileRepository;
import com.training.platform.user.dto.ChangePasswordRequest;
import com.training.platform.user.dto.UserAdminResponse;
import com.training.platform.user.entity.User;
import com.training.platform.user.repository.UserRepository;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LearnerProfileRepository learnerProfileRepository;
    private final TrainerProfileRepository trainerProfileRepository;
    private final TrainerAvailabilityRepository trainerAvailabilityRepository;
    private final TrainingSessionRepository trainingSessionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MessageReadReceiptRepository messageReadReceiptRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final com.training.platform.notification.repository.NotificationRepository notificationRepository;

    public UserAccountService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            LearnerProfileRepository learnerProfileRepository,
            TrainerProfileRepository trainerProfileRepository,
            TrainerAvailabilityRepository trainerAvailabilityRepository,
            TrainingSessionRepository trainingSessionRepository,
            EnrollmentRepository enrollmentRepository,
            ChatMessageRepository chatMessageRepository,
            MessageReadReceiptRepository messageReadReceiptRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            com.training.platform.notification.repository.NotificationRepository notificationRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.learnerProfileRepository = learnerProfileRepository;
        this.trainerProfileRepository = trainerProfileRepository;
        this.trainerAvailabilityRepository = trainerAvailabilityRepository;
        this.trainingSessionRepository = trainingSessionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.messageReadReceiptRepository = messageReadReceiptRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = getByEmail(email);
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new BadRequestException("Old password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<UserAdminResponse> findAllForAdmin() {
        return userRepository.findAll().stream()
                .map(this::toAdminResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserAdminResponse findByIdForAdmin(Long id) {
        return toAdminResponse(getUser(id));
    }

    @Transactional
    public UserAdminResponse activate(Long id) {
        User user = getUser(id);
        user.setEnabled(true);
        return toAdminResponse(userRepository.save(user));
    }

    @Transactional
    public UserAdminResponse deactivate(Long id, String adminEmail) {
        User user = getUser(id);
        if (user.getEmail().equals(adminEmail)) {
            throw new BadRequestException("Admin cannot deactivate himself");
        }
        user.setEnabled(false);
        return toAdminResponse(userRepository.save(user));
    }

    @Transactional
    public UserAdminResponse unlock(Long id) {
        User user = getUser(id);
        user.setAccountLocked(false);
        user.setFailedLoginAttempts(0);
        user.setLockTime(null);
        return toAdminResponse(userRepository.save(user));
    }

    @Transactional
    public void delete(Long id, String adminEmail) {
        User user = getUser(id);
        if (user.getEmail().equals(adminEmail)) {
            throw new BadRequestException("Admin cannot delete himself");
        }

        messageReadReceiptRepository.deleteByUserId(user.getId());
        messageReadReceiptRepository.deleteByMessageSenderId(user.getId());
        chatMessageRepository.deleteBySenderId(user.getId());

        learnerProfileRepository.findByUserId(user.getId()).ifPresent(this::deleteLearnerData);
        trainerProfileRepository.findByUserId(user.getId()).ifPresent(this::deleteTrainerData);

        notificationRepository.deleteByRecipientId(user.getId());
        passwordResetTokenRepository.deleteByUserId(user.getId());
        userRepository.delete(user);
    }

    private void deleteLearnerData(LearnerProfile profile) {
        enrollmentRepository.deleteByLearnerId(profile.getId());
        learnerProfileRepository.delete(profile);
    }

    private void deleteTrainerData(TrainerProfile profile) {
        List<TrainingSession> sessions = trainingSessionRepository.findByTrainerId(profile.getId());
        for (TrainingSession session : sessions) {
            messageReadReceiptRepository.deleteByMessageSessionId(session.getId());
            chatMessageRepository.deleteBySessionId(session.getId());
            enrollmentRepository.deleteBySessionId(session.getId());
        }
        trainingSessionRepository.deleteByTrainerId(profile.getId());
        trainerAvailabilityRepository.deleteByTrainerId(profile.getId());
        trainerProfileRepository.delete(profile);
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    private User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for current account"));
    }

    private UserAdminResponse toAdminResponse(User user) {
        return new UserAdminResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                Boolean.TRUE.equals(user.getAccountLocked()),
                user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts(),
                user.getCreatedAt()
        );
    }
}
