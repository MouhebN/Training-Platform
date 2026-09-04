package com.training.platform.enrollment.dto;

public record EnrollmentCancelResponse(
        Long cancelledEnrollmentId,
        boolean promoted,
        Long promotedEnrollmentId,
        String promotedLearnerFullName,
        String message
) {
}
