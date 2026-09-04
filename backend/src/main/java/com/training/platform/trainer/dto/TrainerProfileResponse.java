package com.training.platform.trainer.dto;

import com.training.platform.skill.dto.SkillResponse;
import com.training.platform.user.dto.UserResponse;
import java.time.Instant;
import java.util.Set;

public record TrainerProfileResponse(
        Long id,
        UserResponse user,
        String phone,
        String bio,
        String cvUrl,
        Integer yearsOfExperience,
        Set<SkillResponse> expertise,
        Double averageRating,
        Boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
