package com.training.platform.chat.dto;

public record UnreadCountResponse(
        Long sessionId,
        long unreadCount
) {
}
