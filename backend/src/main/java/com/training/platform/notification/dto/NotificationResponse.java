package com.training.platform.notification.dto;

import com.training.platform.notification.entity.NotificationType;
import java.time.Instant;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String body,
        String link,
        boolean read,
        Instant createdAt
) {
}
