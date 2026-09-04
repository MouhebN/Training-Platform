package com.training.platform.enrollment.dto;

import com.training.platform.enrollment.entity.EnrollmentStatus;
import com.training.platform.session.entity.SessionStatus;
import java.time.LocalDateTime;

public record EnrollmentResponse(
        Long id,
        Long learnerId,
        String learnerFullName,
        Long sessionId,
        String sessionTitle,
        Long formationId,
        String formationTitle,
        Integer formationSessionCount,
        String trainerFullName,
        EnrollmentStatus status,
        LocalDateTime enrolledAt,
        LocalDateTime sessionStartDate,
        LocalDateTime sessionEndDate,
        Boolean online,
        String location,
        String meetingUrl,
        SessionStatus sessionStatus,
        Integer virtualAttendancePercentage,
        Boolean virtualAttendanceQualified
) {
}
