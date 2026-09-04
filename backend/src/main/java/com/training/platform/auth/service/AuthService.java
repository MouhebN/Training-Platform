package com.training.platform.auth.service;

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
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int RESET_TOKEN_EXPIRATION_MINUTES = 15;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final LearnerProfileService learnerProfileService;
    private final EmailService emailService;

    public AuthService(
            UserRepository userRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserMapper userMapper,
            LearnerProfileService learnerProfileService,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.learnerProfileService = learnerProfileService;
        this.emailService = emailService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BadRequestException("Email is already in use");
        }

        User user = User.builder()
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.password()))
                .role(Role.LEARNER)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);
        learnerProfileService.createEmptyProfile(savedUser);
        String token = jwtService.generateToken(toUserDetails(savedUser));
        return new AuthResponse(token, "Bearer", userMapper.toResponse(savedUser));
    }

    @Transactional(noRollbackFor = BadCredentialsException.class)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!user.isEnabled()) {
            throw new BadRequestException("Your account is banned or blocked. Contact an administrator.");
        }

        if (Boolean.TRUE.equals(user.getAccountLocked())) {
            throw new BadRequestException("Account is locked. Contact an administrator to unlock it.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
            );
        } catch (BadCredentialsException exception) {
            registerFailedLogin(user);
            throw exception;
        }

        resetFailedLoginState(user);
        userRepository.save(user);
        String token = jwtService.generateToken(toUserDetails(user));
        return new AuthResponse(token, "Bearer", userMapper.toResponse(user));
    }

    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("No account found with this email."));

        if (!user.isEnabled()) {
            throw new BadRequestException("Your account is banned or blocked. Contact an administrator.");
        }

        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(RESET_TOKEN_EXPIRATION_MINUTES);
        String token = generateResetToken();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiresAt(expiresAt)
                .used(false)
                .build();
        passwordResetTokenRepository.save(resetToken);
        emailService.sendPasswordResetToken(user.getEmail(), token);
        log.info("Password reset token generated and sent to {}", email);
        return new ForgotPasswordResponse(
                "A password reset token has been sent to your email.",
                null,
                expiresAt
        );
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.email().trim().toLowerCase();
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.token().trim())
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        if (Boolean.TRUE.equals(resetToken.getUsed()) || resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Invalid or expired reset token");
        }

        User user = resetToken.getUser();
        if (!user.getEmail().equalsIgnoreCase(email)) {
            throw new BadRequestException("Email does not match the account for this reset token.");
        }

        if (!user.isEnabled()) {
            throw new BadRequestException("Your account is banned or blocked. Contact an administrator.");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        resetFailedLoginState(user);
        user.setAccountLocked(false);
        resetToken.setUsed(true);
        userRepository.save(user);
        passwordResetTokenRepository.save(resetToken);
    }

    private void registerFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts();
        attempts++;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setAccountLocked(true);
            user.setLockTime(LocalDateTime.now());
        }
        userRepository.save(user);
    }

    private void resetFailedLoginState(User user) {
        user.setFailedLoginAttempts(0);
        user.setLockTime(null);
    }

    private String generateResetToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private UserDetails toUserDetails(User user) {
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .disabled(!user.isEnabled())
                .accountLocked(Boolean.TRUE.equals(user.getAccountLocked()))
                .build();
    }
}
