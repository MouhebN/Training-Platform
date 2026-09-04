package com.training.platform.attendance.dto;

import java.util.List;

public record ClassroomAttendanceReportResponse(
        Long sessionId,
        long trainerActiveSeconds,
        int attendanceThresholdPercent,
        List<ClassroomAttendanceEntryResponse> learners
) {
}
