package com.training.platform.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.training.platform.auth.repository.PasswordResetTokenRepository;
import com.training.platform.chat.repository.ChatMessageRepository;
import com.training.platform.chat.repository.MessageReadReceiptRepository;
import com.training.platform.common.exception.BadRequestException;
import com.training.platform.enrollment.repository.EnrollmentRepository;
import com.training.platform.learner.repository.LearnerProfileRepository;
import com.training.platform.session.repository.TrainingSessionRepository;
import com.training.platform.trainer.repository.TrainerAvailabilityRepository;
import com.training.platform.trainer.repository.TrainerProfileRepository;
import com.training.platform.user.dto.ChangePasswordRequest;
import com.training.platform.user.entity.Role;
import com.training.platform.user.entity.User;
import com.training.platform.user.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private LearnerProfileRepository learnerProfileRepository;

    @Mock
    private TrainerProfileRepository trainerProfileRepository;

    @Mock
    private TrainerAvailabilityRepository trainerAvailabilityRepository;

    @Mock
    private TrainingSessionRepository trainingSessionRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private MessageReadReceiptRepository messageReadReceiptRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    private UserAccountService userAccountService;

    @BeforeEach
    void setUp() {
        userAccountService = new UserAccountService(
                userRepository,
                passwordEncoder,
                learnerProfileRepository,
                trainerProfileRepository,
                trainerAvailabilityRepository,
                trainingSessionRepository,
                enrollmentRepository,
                chatMessageRepository,
                messageReadReceiptRepository,
                passwordResetTokenRepository,
                org.mockito.Mockito.mock(com.training.platform.notification.repository.NotificationRepository.class)
        );
    }

    @Test
    void changePasswordEncodesNewPasswordWhenOldPasswordMatches() {
        User user = user(1L, "learner@test.com", Role.LEARNER);
        when(userRepository.findByEmail("learner@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldpass", "encoded-old")).thenReturn(true);
        when(passwordEncoder.encode("newpass123")).thenReturn("encoded-new");

        userAccountService.changePassword("learner@test.com", new ChangePasswordRequest("oldpass", "newpass123"));

        assertThat(user.getPassword()).isEqualTo("encoded-new");
        verify(userRepository).save(user);
    }

    @Test
    void changePasswordRejectsWrongOldPassword() {
        User user = user(1L, "learner@test.com", Role.LEARNER);
        when(userRepository.findByEmail("learner@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded-old")).thenReturn(false);

        assertThatThrownBy(() -> userAccountService.changePassword(
                "learner@test.com",
                new ChangePasswordRequest("wrong", "newpass123")
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Old password is incorrect");
    }

    @Test
    void deactivateRejectsCurrentAdmin() {
        User admin = user(1L, "admin@training.com", Role.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> userAccountService.deactivate(1L, "admin@training.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Admin cannot deactivate himself");
    }

    @Test
    void deleteRejectsCurrentAdmin() {
        User admin = user(1L, "admin@training.com", Role.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> userAccountService.delete(1L, "admin@training.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Admin cannot delete himself");
    }

    private User user(Long id, String email, Role role) {
        return User.builder()
                .id(id)
                .firstName("Test")
                .lastName("User")
                .email(email)
                .password("encoded-old")
                .role(role)
                .enabled(true)
                .failedLoginAttempts(0)
                .accountLocked(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
