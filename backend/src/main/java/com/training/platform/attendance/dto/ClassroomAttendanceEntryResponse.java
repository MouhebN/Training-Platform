package com.training.platform.attendance.dto;

import com.training.platform.enrollment.entity.EnrollmentStatus;

public record ClassroomAttendanceEntryResponse(
        Long enrollmentId,
        Long learnerId,
        String learnerFullName,
        EnrollmentStatus enrollmentStatus,
        boolean connected,
        long trackedSeconds,
        long trainerActiveSeconds,
        int attendancePercentage,
        boolean qualified
) {
}
