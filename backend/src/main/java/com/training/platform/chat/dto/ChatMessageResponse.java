package com.training.platform.chat.dto;

import com.training.platform.chat.entity.ChatMessageType;
import com.training.platform.user.entity.Role;
import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long id,
        Long sessionId,
        Long senderId,
        String senderFullName,
        Role senderRole,
        String content,
        ChatMessageType messageType,
        LocalDateTime createdAt,
        boolean mine,
        long readByCount
) {
}
