package com.training.platform.attendance.dto;

public record ClassroomContextResponse(
        Long sessionId,
        String sessionTitle,
        String jitsiDomain,
        String roomName,
        String displayName,
        boolean moderator,
        int heartbeatIntervalSec,
        int attendanceThresholdPercent
) {
}
