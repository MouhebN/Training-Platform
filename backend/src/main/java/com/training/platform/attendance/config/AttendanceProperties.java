package com.training.platform.attendance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.attendance")
public record AttendanceProperties(
        int thresholdPercent,
        int heartbeatIntervalSec,
        int staleAfterSec,
        String jitsiDomain
) {
    public AttendanceProperties {
        if (thresholdPercent <= 0) {
            thresholdPercent = 70;
        }
        if (heartbeatIntervalSec <= 0) {
            heartbeatIntervalSec = 30;
        }
        if (staleAfterSec <= 0) {
            staleAfterSec = 90;
        }
        if (jitsiDomain == null || jitsiDomain.isBlank()) {
            jitsiDomain = "meet.jit.si";
        }
    }
}
