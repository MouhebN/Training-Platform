package com.training.platform.notification.repository;

import com.training.platform.notification.entity.AppNotification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<AppNotification, Long> {

    List<AppNotification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    long countByRecipientIdAndReadFalse(Long recipientId);

    Optional<AppNotification> findByIdAndRecipientId(Long id, Long recipientId);

    void deleteByRecipientId(Long recipientId);
}
