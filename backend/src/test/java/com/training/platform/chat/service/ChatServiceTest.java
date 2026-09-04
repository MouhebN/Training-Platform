package com.training.platform.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.training.platform.catalogue.category.entity.Category;
import com.training.platform.catalogue.formation.entity.Formation;
import com.training.platform.catalogue.formation.entity.FormationLevel;
import com.training.platform.chat.dto.ChatMessageRequest;
import com.training.platform.chat.entity.ChatMessage;
import com.training.platform.chat.repository.ChatMessageRepository;
import com.training.platform.chat.repository.MessageReadReceiptRepository;
import com.training.platform.enrollment.entity.Enrollment;
import com.training.platform.enrollment.entity.EnrollmentStatus;
import com.training.platform.enrollment.repository.EnrollmentRepository;
import com.training.platform.learner.entity.LearnerLevel;
import com.training.platform.learner.entity.LearnerProfile;
import com.training.platform.session.entity.SessionStatus;
import com.training.platform.session.entity.TrainingSession;
import com.training.platform.session.service.TrainingSessionService;
import com.training.platform.trainer.entity.TrainerProfile;
import com.training.platform.user.entity.Role;
import com.training.platform.user.entity.User;
import com.training.platform.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatMessageRepository messageRepository;

    @Mock
    private MessageReadReceiptRepository readReceiptRepository;

    @Mock
    private TrainingSessionService sessionService;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private UserRepository userRepository;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(
                messageRepository,
                readReceiptRepository,
                sessionService,
                enrollmentRepository,
                userRepository,
                org.mockito.Mockito.mock(com.training.platform.notification.service.NotificationService.class)
        );
    }

    @Test
    void enrolledLearnerCanAccessChat() {
        User learner = user(4L, "learner@test.com", Role.LEARNER);
        TrainingSession session = session(user(2L, "trainer@test.com", Role.TRAINER));
        when(userRepository.findById(4L)).thenReturn(Optional.of(learner));
        when(sessionService.getSession(5L)).thenReturn(session);
        when(enrollmentRepository.findBySessionIdOrderByEnrolledAtDesc(5L)).thenReturn(List.of(enrollment(learner, session, EnrollmentStatus.CONFIRMED)));

        assertThat(chatService.canAccessSessionChat(5L, 4L)).isTrue();
    }

    @Test
    void assignedTrainerCanAccessChat() {
        User trainer = user(2L, "trainer@test.com", Role.TRAINER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(trainer));
        when(sessionService.getSession(5L)).thenReturn(session(trainer));

        assertThat(chatService.canAccessSessionChat(5L, 2L)).isTrue();
    }

    @Test
    void unrelatedLearnerCannotAccessChat() {
        User learner = user(8L, "other@test.com", Role.LEARNER);
        when(userRepository.findById(8L)).thenReturn(Optional.of(learner));
        when(sessionService.getSession(5L)).thenReturn(session(user(2L, "trainer@test.com", Role.TRAINER)));
        when(enrollmentRepository.findBySessionIdOrderByEnrolledAtDesc(5L)).thenReturn(List.of());

        assertThat(chatService.canAccessSessionChat(5L, 8L)).isFalse();
    }

    @Test
    void sendPersistsMessage() {
        User learner = user(4L, "learner@test.com", Role.LEARNER);
        TrainingSession session = session(user(2L, "trainer@test.com", Role.TRAINER));
        when(userRepository.findByEmail("learner@test.com")).thenReturn(Optional.of(learner));
        when(sessionService.getSession(5L)).thenReturn(session);
        when(enrollmentRepository.findBySessionIdOrderByEnrolledAtDesc(5L)).thenReturn(List.of(enrollment(learner, session, EnrollmentStatus.CONFIRMED)));
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            message.setId(11L);
            message.setCreatedAt(LocalDateTime.now());
            return message;
        });
        when(readReceiptRepository.countByMessageId(11L)).thenReturn(0L);

        var response = chatService.send(5L, new ChatMessageRequest("Hello trainer"), "learner@test.com");

        assertThat(response.content()).isEqualTo("Hello trainer");
        assertThat(response.mine()).isTrue();
        verify(messageRepository).save(any(ChatMessage.class));
    }

    @Test
    void unreadCountIncreasesForOtherUser() {
        User trainer = user(2L, "trainer@test.com", Role.TRAINER);
        when(userRepository.findByEmail("trainer@test.com")).thenReturn(Optional.of(trainer));
        when(sessionService.getSession(5L)).thenReturn(session(trainer));
        when(messageRepository.countUnread(5L, 2L)).thenReturn(3L);

        assertThat(chatService.unreadCount(5L, "trainer@test.com").unreadCount()).isEqualTo(3);
    }

    @Test
    void markAsReadCreatesReceiptsAndReturnsZero() {
        User trainer = user(2L, "trainer@test.com", Role.TRAINER);
        ChatMessage message = ChatMessage.builder()
                .id(20L)
                .session(session(trainer))
                .sender(user(4L, "learner@test.com", Role.LEARNER))
                .senderFullName("Learner One")
                .senderRole(Role.LEARNER)
                .content("Question")
                .createdAt(LocalDateTime.now())
                .build();
        when(userRepository.findByEmail("trainer@test.com")).thenReturn(Optional.of(trainer));
        when(sessionService.getSession(5L)).thenReturn(session(trainer));
        when(messageRepository.findBySessionIdAndSenderIdNot(5L, 2L)).thenReturn(List.of(message));
        when(readReceiptRepository.findReadMessageIds(2L, List.of(20L))).thenReturn(Set.of());

        var response = chatService.markAsRead(5L, "trainer@test.com");

        assertThat(response.unreadCount()).isZero();
        verify(readReceiptRepository).save(any());
    }

    @Test
    void sendRejectsUnrelatedLearner() {
        User learner = user(8L, "other@test.com", Role.LEARNER);
        when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(learner));
        when(sessionService.getSession(5L)).thenReturn(session(user(2L, "trainer@test.com", Role.TRAINER)));
        when(enrollmentRepository.findBySessionIdOrderByEnrolledAtDesc(5L)).thenReturn(List.of());

        assertThatThrownBy(() -> chatService.send(5L, new ChatMessageRequest("Hi"), "other@test.com"))
                .isInstanceOf(AccessDeniedException.class);
    }

    private Enrollment enrollment(User learnerUser, TrainingSession session, EnrollmentStatus status) {
        return Enrollment.builder()
                .id(1L)
                .learner(LearnerProfile.builder()
                        .id(4L)
                        .user(learnerUser)
                        .currentLevel(LearnerLevel.BEGINNER)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build())
                .session(session)
                .status(status)
                .enrolledAt(LocalDateTime.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private TrainingSession session(User trainerUser) {
        return TrainingSession.builder()
                .id(5L)
                .formation(Formation.builder()
                        .id(1L)
                        .title("Spring Boot")
                        .price(BigDecimal.valueOf(200))
                        .level(FormationLevel.BEGINNER)
                        .durationHours(20)
                        .active(true)
                        .category(Category.builder().id(1L).name("IT").createdAt(Instant.now()).updatedAt(Instant.now()).build())
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build())
                .trainer(TrainerProfile.builder()
                        .id(2L)
                        .user(trainerUser)
                        .yearsOfExperience(3)
                        .averageRating(4.0)
                        .active(true)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build())
                .title("Spring Boot Session")
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusHours(4))
                .capacity(10)
                .online(true)
                .status(SessionStatus.OPEN)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private User user(Long id, String email, Role role) {
        return User.builder()
                .id(id)
                .firstName(role == Role.TRAINER ? "Trainer" : "Learner")
                .lastName("One")
                .email(email)
                .password("encoded")
                .role(role)
                .enabled(true)
                .failedLoginAttempts(0)
                .accountLocked(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
