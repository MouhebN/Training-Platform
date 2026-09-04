package com.training.platform.chat.controller;

import com.training.platform.chat.dto.ChatMessageResponse;
import com.training.platform.chat.dto.UnreadCountResponse;
import com.training.platform.chat.service.ChatService;
import com.training.platform.common.response.ApiResponse;
import java.security.Principal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatRestController {

    private final ChatService chatService;

    public ChatRestController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/api/sessions/{sessionId}/messages")
    @PreAuthorize("@chatService.canAccessSessionChat(#sessionId, authentication.name)")
    public ApiResponse<Page<ChatMessageResponse>> history(
            @PathVariable Long sessionId,
            Pageable pageable,
            Principal principal
    ) {
        return ApiResponse.success("Messages retrieved", chatService.history(sessionId, principal.getName(), pageable));
    }

    @PostMapping("/api/sessions/{sessionId}/messages/read")
    @PreAuthorize("@chatService.canAccessSessionChat(#sessionId, authentication.name)")
    public ApiResponse<UnreadCountResponse> markAsRead(@PathVariable Long sessionId, Principal principal) {
        return ApiResponse.success("Messages marked as read", chatService.markAsRead(sessionId, principal.getName()));
    }

    @GetMapping("/api/sessions/{sessionId}/messages/unread-count")
    @PreAuthorize("@chatService.canAccessSessionChat(#sessionId, authentication.name)")
    public ApiResponse<UnreadCountResponse> unreadCount(@PathVariable Long sessionId, Principal principal) {
        return ApiResponse.success("Unread count retrieved", chatService.unreadCount(sessionId, principal.getName()));
    }
}
