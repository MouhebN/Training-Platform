package com.training.platform.auth.dto;

import java.time.LocalDateTime;

public record ForgotPasswordResponse(
        String message,
        String resetToken,
        LocalDateTime expiresAt
) {
}
