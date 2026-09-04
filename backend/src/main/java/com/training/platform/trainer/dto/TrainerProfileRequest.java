package com.training.platform.trainer.dto;

import jakarta.validation.constraints.Min;
import java.util.Set;

public record TrainerProfileRequest(
        String phone,
        String bio,
        String cvUrl,

        @Min(value = 0, message = "Years of experience must be greater than or equal to 0")
        Integer yearsOfExperience,

        Set<Long> expertiseSkillIds,
        Boolean active
) {
}
