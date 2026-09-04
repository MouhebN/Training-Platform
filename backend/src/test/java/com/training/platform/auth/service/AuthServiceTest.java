package com.training.platform.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.training.platform.auth.dto.AuthResponse;
import com.training.platform.auth.dto.ForgotPasswordRequest;
import com.training.platform.auth.dto.ForgotPasswordResponse;
import com.training.platform.auth.dto.LoginRequest;
import com.training.platform.auth.dto.RegisterRequest;
import com.training.platform.auth.dto.ResetPasswordRequest;
import com.training.platform.auth.entity.PasswordResetToken;
import com.training.platform.auth.repository.PasswordResetTokenRepository;
import com.training.platform.common.exception.BadRequestException;
import com.training.platform.common.security.JwtService;
import com.training.platform.common.service.EmailService;
import com.training.platform.learner.service.LearnerProfileService;
import com.training.platform.user.entity.Role;
import com.training.platform.user.entity.User;
import com.training.platform.user.repository.UserRepository;
import com.training.platform.user.service.UserMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private LearnerProfileService learnerProfileService;

    @Mock
    private EmailService emailService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                passwordResetTokenRepository,
                passwordEncoder,
                authenticationManager,
                jwtService,
                new UserMapper(),
                learnerProfileService,
                emailService
        );
    }

    @Test
    void registerCreatesEnabledLearnerWithEncodedPassword() {
        RegisterRequest request = new RegisterRequest(
                " Mahdi ",
                " Naddari ",
                "MAHDI@example.com",
                "secret123"
        );
        User savedUser = user(1L, "Mahdi", "Naddari", "mahdi@example.com", "encoded", Role.LEARNER);

        when(userRepository.existsByEmail("mahdi@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any())).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        verify(learnerProfileService).createEmptyProfile(savedUser);
        User userToSave = userCaptor.getValue();

        assertThat(userToSave.getFirstName()).isEqualTo("Mahdi");
        assertThat(userToSave.getLastName()).isEqualTo("Naddari");
        assertThat(userToSave.getEmail()).isEqualTo("mahdi@example.com");
        assertThat(userToSave.getPassword()).isEqualTo("encoded");
        assertThat(userToSave.getRole()).isEqualTo(Role.LEARNER);
        assertThat(userToSave.isEnabled()).isTrue();
        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user().email()).isEqualTo("mahdi@example.com");
    }

    @Test
    void registerRejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("Mahdi", "Naddari", "mahdi@example.com", "secret123");
        when(userRepository.existsByEmail("mahdi@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Email is already in use");
    }

    @Test
    void loginAuthenticatesAndReturnsToken() {
        LoginRequest request = new LoginRequest("ADMIN@training.com", "admin123");
        User admin = user(1L, "Platform", "Admin", "admin@training.com", "encoded", Role.ADMIN);

        when(userRepository.findByEmail("admin@training.com")).thenReturn(Optional.of(admin));
        when(jwtService.generateToken(any())).thenReturn("admin-token");

        AuthResponse response = authService.login(request);

        verify(authenticationManager).authenticate(argThat(authentication ->
                authentication instanceof UsernamePasswordAuthenticationToken
                        && authentication.getPrincipal().equals("admin@training.com")
                        && authentication.getCredentials().equals("admin123")
        ));
        assertThat(response.token()).isEqualTo("admin-token");
        assertThat(response.user().role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void failedLoginIncrementsAttemptsAndLocksAfterFiveFailures() {
        LoginRequest request = new LoginRequest("user@test.com", "wrong");
        User user = user(1L, "Test", "User", "user@test.com", "encoded", Role.LEARNER);
        user.setFailedLoginAttempts(4);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        org.mockito.Mockito.doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager)
                .authenticate(any());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(user.getAccountLocked()).isTrue();
        assertThat(user.getLockTime()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    void loginRejectsDisabledAccount() {
        LoginRequest request = new LoginRequest("banned@test.com", "password123");
        User user = user(1L, "Banned", "User", "banned@test.com", "encoded", Role.LEARNER);
        user.setEnabled(false);

        when(userRepository.findByEmail("banned@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Your account is banned or blocked. Contact an administrator.");
    }

    @Test
    void loginRejectsLockedAccount() {
        LoginRequest request = new LoginRequest("locked@test.com", "password123");
        User user = user(1L, "Locked", "User", "locked@test.com", "encoded", Role.LEARNER);
        user.setAccountLocked(true);

        when(userRepository.findByEmail("locked@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Account is locked. Contact an administrator to unlock it.");
    }

    @Test
    void forgotPasswordGeneratesTokenAndEmailsItForExistingEmail() {
        User user = user(1L, "Test", "User", "user@test.com", "encoded", Role.LEARNER);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        ForgotPasswordResponse response = authService.forgotPassword(new ForgotPasswordRequest("user@test.com"));

        assertThat(response.resetToken()).isNull();
        assertThat(response.expiresAt()).isAfter(LocalDateTime.now());
        assertThat(response.message()).isEqualTo("A password reset token has been sent to your email.");
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetToken(org.mockito.Mockito.eq("user@test.com"), any());
    }

    @Test
    void forgotPasswordRejectsUnknownEmail() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.forgotPassword(new ForgotPasswordRequest("unknown@test.com")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("No account found with this email.");
    }

    @Test
    void resetPasswordEncodesNewPasswordAndMarksTokenUsed() {
        User user = user(1L, "Test", "User", "user@test.com", "old", Role.LEARNER);
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(1L)
                .token("token")
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();

        when(passwordResetTokenRepository.findByToken("token")).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("newpass123")).thenReturn("encoded-new");

        authService.resetPassword(new ResetPasswordRequest("user@test.com", "token", "newpass123"));

        assertThat(user.getPassword()).isEqualTo("encoded-new");
        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getAccountLocked()).isFalse();
        assertThat(resetToken.getUsed()).isTrue();
        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).save(resetToken);
    }

    @Test
    void resetPasswordRejectsMismatchedEmail() {
        User user = user(1L, "Test", "User", "user@test.com", "old", Role.LEARNER);
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(1L)
                .token("token")
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();

        when(passwordResetTokenRepository.findByToken("token")).thenReturn(Optional.of(resetToken));

        assertThatThrownBy(() ->
                authService.resetPassword(new ResetPasswordRequest("other@test.com", "token", "newpass123"))
        )
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Email does not match the account for this reset token.");
    }

    private User user(Long id, String firstName, String lastName, String email, String password, Role role) {
        return User.builder()
                .id(id)
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .password(password)
                .role(role)
                .enabled(true)
                .failedLoginAttempts(0)
                .accountLocked(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
