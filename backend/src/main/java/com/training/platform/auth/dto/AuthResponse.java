package com.training.platform.auth.dto;

import com.training.platform.user.dto.UserResponse;

public record AuthResponse(
        String token,
        String tokenType,
        UserResponse user
) {
}
