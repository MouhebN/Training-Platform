package com.training.platform.session.dto;

import com.training.platform.session.entity.SessionStatus;
import jakarta.validation.constraints.NotNull;

public record TrainingSessionStatusUpdateRequest(
        @NotNull(message = "Session status is required")
        SessionStatus status
) {
}
