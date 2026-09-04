package com.training.platform.learner.dto;

public record LearnerProfileUpdateResponse(
        LearnerProfileResponse profile,
        String token
) {
}
