package com.training.platform.notification.controller;

import com.training.platform.common.response.ApiResponse;
import com.training.platform.notification.dto.NotificationResponse;
import com.training.platform.notification.service.NotificationService;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<NotificationResponse>> mine(Principal principal) {
        return ApiResponse.success("Notifications retrieved", notificationService.findMine(principal.getName()));
    }

    @GetMapping("/me/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Long>> unreadCount(Principal principal) {
        return ApiResponse.success(
                "Unread notification count retrieved",
                Map.of("unreadCount", notificationService.unreadCount(principal.getName()))
        );
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<NotificationResponse> markRead(@PathVariable Long id, Principal principal) {
        return ApiResponse.success("Notification marked as read", notificationService.markRead(id, principal.getName()));
    }

    @PostMapping("/me/read-all")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> markAllRead(Principal principal) {
        notificationService.markAllRead(principal.getName());
        return ApiResponse.success("All notifications marked as read", null);
    }
}
