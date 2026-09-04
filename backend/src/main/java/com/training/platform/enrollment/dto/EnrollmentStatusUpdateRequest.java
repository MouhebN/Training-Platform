package com.training.platform.enrollment.dto;

import com.training.platform.enrollment.entity.EnrollmentStatus;
import jakarta.validation.constraints.NotNull;

public record EnrollmentStatusUpdateRequest(
        @NotNull(message = "Enrollment status is required")
        EnrollmentStatus status
) {
}
