package com.training.platform.skill.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SkillRequest(
        @NotBlank(message = "Skill name is required")
        @Size(max = 120, message = "Skill name must not exceed 120 characters")
        String name,

        String description
) {
}
