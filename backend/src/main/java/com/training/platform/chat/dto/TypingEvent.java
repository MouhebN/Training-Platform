package com.training.platform.chat.dto;

public record TypingEvent(
        Long sessionId,
        Long userId,
        String fullName,
        boolean typing
) {
}
