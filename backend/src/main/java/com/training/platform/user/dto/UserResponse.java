package com.training.platform.user.dto;

import com.training.platform.user.entity.Role;
import java.time.Instant;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        Role role,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}
