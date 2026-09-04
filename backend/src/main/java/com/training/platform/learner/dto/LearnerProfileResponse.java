package com.training.platform.learner.dto;

import com.training.platform.learner.entity.LearnerLevel;
import com.training.platform.skill.dto.SkillResponse;
import com.training.platform.user.dto.UserResponse;
import java.time.Instant;
import java.util.Set;

public record LearnerProfileResponse(
        Long id,
        UserResponse user,
        String phone,
        String bio,
        LearnerLevel currentLevel,
        Set<SkillResponse> skills,
        String learningGoals,
        Instant createdAt,
        Instant updatedAt
) {
}
