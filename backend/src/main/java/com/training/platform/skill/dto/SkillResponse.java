package com.training.platform.skill.dto;

import java.time.Instant;

public record SkillResponse(
        Long id,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
