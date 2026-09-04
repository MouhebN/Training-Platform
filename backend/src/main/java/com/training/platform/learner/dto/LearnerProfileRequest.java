package com.training.platform.learner.dto;

import com.training.platform.learner.entity.LearnerLevel;
import jakarta.validation.constraints.Email;
import java.util.Set;

public record LearnerProfileRequest(
        @Email(message = "Email must be valid")
        String email,
        String phone,
        String bio,
        LearnerLevel currentLevel,
        Set<Long> skillIds,
        String learningGoals
) {
}
