package com.training.platform.notification.service;

import com.training.platform.notification.dto.NotificationResponse;
import com.training.platform.notification.entity.AppNotification;
import com.training.platform.notification.entity.NotificationType;
import com.training.platform.notification.repository.NotificationRepository;
import com.training.platform.user.entity.Role;
import com.training.platform.user.entity.User;
import com.training.platform.user.repository.UserRepository;
import com.training.platform.common.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public NotificationResponse push(
            User recipient,
            NotificationType type,
            String title,
            String body,
            String link
    ) {
        AppNotification saved = notificationRepository.save(AppNotification.builder()
                .recipient(recipient)
                .type(type)
                .title(title)
                .body(body)
                .link(link)
                .read(false)
                .build());
        NotificationResponse response = toResponse(saved);
        messagingTemplate.convertAndSend("/topic/users/" + recipient.getId() + "/notifications", response);
        return response;
    }

    @Transactional
    public void notifyAdmins(NotificationType type, String title, String body, String link) {
        userRepository.findByRoleAndEnabledTrue(Role.ADMIN)
                .forEach(admin -> push(admin, type, title, body, link));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> findMine(String email) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(getUser(email).getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(String email) {
        return notificationRepository.countByRecipientIdAndReadFalse(getUser(email).getId());
    }

    @Transactional
    public NotificationResponse markRead(Long id, String email) {
        AppNotification notification = notificationRepository.findByIdAndRecipientId(id, getUser(email).getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notification.setRead(true);
        return toResponse(notificationRepository.save(notification));
    }

    @Transactional
    public void markAllRead(String email) {
        List<AppNotification> notifications = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(getUser(email).getId());
        notifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(notifications);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private NotificationResponse toResponse(AppNotification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getLink(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
