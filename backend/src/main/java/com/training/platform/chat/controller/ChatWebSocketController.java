package com.training.platform.chat.controller;

import com.training.platform.chat.dto.ChatMessageRequest;
import com.training.platform.chat.dto.ChatMessageResponse;
import com.training.platform.chat.dto.TypingEvent;
import com.training.platform.chat.dto.UnreadCountResponse;
import com.training.platform.chat.service.ChatService;
import java.security.Principal;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/sessions/{sessionId}/chat.send")
    public void sendMessage(
            @DestinationVariable Long sessionId,
            @Payload ChatMessageRequest request,
            Principal principal
    ) {
        ChatMessageResponse response = chatService.send(sessionId, request, principal.getName());
        messagingTemplate.convertAndSend("/topic/sessions/" + sessionId + "/chat", response);
    }

    @MessageMapping("/sessions/{sessionId}/chat.typing")
    public void typing(
            @DestinationVariable Long sessionId,
            @Payload TypingEvent event,
            Principal principal
    ) {
        if (chatService.canAccessSessionChat(sessionId, principal.getName())) {
            messagingTemplate.convertAndSend("/topic/sessions/" + sessionId + "/typing", event);
        }
    }

    @MessageMapping("/sessions/{sessionId}/chat.read")
    public void read(@DestinationVariable Long sessionId, Principal principal) {
        UnreadCountResponse response = chatService.markAsRead(sessionId, principal.getName());
        messagingTemplate.convertAndSend("/topic/sessions/" + sessionId + "/read", response);
    }
}
