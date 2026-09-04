package com.training.platform.chat.service;

import com.training.platform.chat.dto.ChatMessageRequest;
import com.training.platform.chat.dto.ChatMessageResponse;
import com.training.platform.chat.dto.UnreadCountResponse;
import com.training.platform.chat.entity.ChatMessage;
import com.training.platform.chat.entity.ChatMessageType;
import com.training.platform.chat.entity.MessageReadReceipt;
import com.training.platform.chat.repository.ChatMessageRepository;
import com.training.platform.chat.repository.MessageReadReceiptRepository;
import com.training.platform.common.exception.BadRequestException;
import com.training.platform.common.exception.ResourceNotFoundException;
import com.training.platform.enrollment.entity.EnrollmentStatus;
import com.training.platform.enrollment.repository.EnrollmentRepository;
import com.training.platform.notification.entity.NotificationType;
import com.training.platform.notification.service.NotificationService;
import com.training.platform.session.entity.TrainingSession;
import com.training.platform.session.service.TrainingSessionService;
import com.training.platform.user.entity.Role;
import com.training.platform.user.entity.User;
import com.training.platform.user.repository.UserRepository;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {

    private final ChatMessageRepository messageRepository;
    private final MessageReadReceiptRepository readReceiptRepository;
    private final TrainingSessionService sessionService;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public ChatService(
            ChatMessageRepository messageRepository,
            MessageReadReceiptRepository readReceiptRepository,
            TrainingSessionService sessionService,
            EnrollmentRepository enrollmentRepository,
            UserRepository userRepository,
            NotificationService notificationService
    ) {
        this.messageRepository = messageRepository;
        this.readReceiptRepository = readReceiptRepository;
        this.sessionService = sessionService;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public Page<ChatMessageResponse> history(Long sessionId, String email, Pageable pageable) {
        User user = getUser(email);
        assertCanAccessSessionChat(sessionId, user);
        return messageRepository.findBySessionIdOrderByCreatedAtDesc(sessionId, pageable)
                .map(message -> toResponse(message, user));
    }

    @Transactional
    public ChatMessageResponse send(Long sessionId, ChatMessageRequest request, String email) {
        if (request.content() == null || request.content().isBlank()) {
            throw new BadRequestException("Message content is required");
        }
        User user = getUser(email);
        TrainingSession session = sessionService.getSession(sessionId);
        assertCanAccessSessionChat(sessionId, user);
        ChatMessage message = ChatMessage.builder()
                .session(session)
                .sender(user)
                .senderFullName(user.getFirstName() + " " + user.getLastName())
                .senderRole(user.getRole())
                .content(request.content().trim())
                .messageType(ChatMessageType.TEXT)
                .build();
        ChatMessage saved = messageRepository.save(message);
        notifyChatRecipients(session, user, saved.getContent());
        return toResponse(saved, user);
    }

    private void notifyChatRecipients(TrainingSession session, User sender, String content) {
        String preview = content.length() > 80 ? content.substring(0, 77) + "..." : content;
        String body = sender.getFirstName() + " " + sender.getLastName() + ": " + preview;
        sessionChatRecipients(session).stream()
                .filter(recipient -> !recipient.getId().equals(sender.getId()))
                .forEach(recipient -> notificationService.push(
                        recipient,
                        NotificationType.CHAT_MESSAGE,
                        "New message in " + session.getTitle(),
                        body,
                        chatLink(recipient, session.getId())
                ));
    }

    private List<User> sessionChatRecipients(TrainingSession session) {
        List<User> recipients = new java.util.ArrayList<>();
        recipients.add(session.getTrainer().getUser());
        enrollmentRepository.findBySessionIdOrderByEnrolledAtDesc(session.getId()).stream()
                .filter(enrollment -> List.of(
                        EnrollmentStatus.CONFIRMED,
                        EnrollmentStatus.WAITLISTED,
                        EnrollmentStatus.COMPLETED
                ).contains(enrollment.getStatus()))
                .map(enrollment -> enrollment.getLearner().getUser())
                .forEach(recipients::add);
        return recipients;
    }

    private String chatLink(User user, Long sessionId) {
        if (user.getRole() == Role.TRAINER) {
            return "/trainer/sessions/" + sessionId + "/chat";
        }
        return "/learner/sessions/" + sessionId + "/chat";
    }

    @Transactional
    public UnreadCountResponse markAsRead(Long sessionId, String email) {
        User user = getUser(email);
        assertCanAccessSessionChat(sessionId, user);
        List<ChatMessage> unreadCandidates = messageRepository.findBySessionIdAndSenderIdNot(sessionId, user.getId());
        if (unreadCandidates.isEmpty()) {
            return new UnreadCountResponse(sessionId, 0);
        }
        Set<Long> alreadyRead = readReceiptRepository.findReadMessageIds(
                user.getId(),
                unreadCandidates.stream().map(ChatMessage::getId).toList()
        );
        unreadCandidates.stream()
                .filter(message -> !alreadyRead.contains(message.getId()))
                .map(message -> MessageReadReceipt.builder().message(message).user(user).build())
                .forEach(readReceiptRepository::save);
        return new UnreadCountResponse(sessionId, 0);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(Long sessionId, String email) {
        User user = getUser(email);
        assertCanAccessSessionChat(sessionId, user);
        return new UnreadCountResponse(sessionId, messageRepository.countUnread(sessionId, user.getId()));
    }

    @Transactional(readOnly = true)
    public boolean canAccessSessionChat(Long sessionId, Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        return user != null && canAccessSessionChat(sessionId, user);
    }

    @Transactional(readOnly = true)
    public boolean canAccessSessionChat(Long sessionId, String email) {
        return canAccessSessionChat(sessionId, getUser(email));
    }

    private boolean canAccessSessionChat(Long sessionId, User user) {
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        TrainingSession session = sessionService.getSession(sessionId);
        if (user.getRole() == Role.TRAINER && session.getTrainer().getUser().getId().equals(user.getId())) {
            return true;
        }
        if (user.getRole() == Role.LEARNER) {
            return enrollmentRepository.findBySessionIdOrderByEnrolledAtDesc(sessionId).stream()
                    .anyMatch(enrollment ->
                            enrollment.getLearner().getUser().getId().equals(user.getId())
                                    && List.of(EnrollmentStatus.CONFIRMED, EnrollmentStatus.WAITLISTED, EnrollmentStatus.COMPLETED)
                                    .contains(enrollment.getStatus())
                    );
        }
        return false;
    }

    private void assertCanAccessSessionChat(Long sessionId, User user) {
        if (!canAccessSessionChat(sessionId, user)) {
            throw new AccessDeniedException("You cannot access this session chat");
        }
    }

    private ChatMessageResponse toResponse(ChatMessage message, User currentUser) {
        return new ChatMessageResponse(
                message.getId(),
                message.getSession().getId(),
                message.getSender().getId(),
                message.getSenderFullName(),
                message.getSenderRole(),
                message.getContent(),
                message.getMessageType(),
                message.getCreatedAt(),
                message.getSender().getId().equals(currentUser.getId()),
                readReceiptRepository.countByMessageId(message.getId())
        );
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
