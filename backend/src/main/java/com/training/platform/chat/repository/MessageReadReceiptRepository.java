package com.training.platform.chat.repository;

import com.training.platform.chat.entity.MessageReadReceipt;
import java.util.Collection;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageReadReceiptRepository extends JpaRepository<MessageReadReceipt, Long> {

    boolean existsByMessageIdAndUserId(Long messageId, Long userId);

    long countByMessageId(Long messageId);

    @Query("select r.message.id from MessageReadReceipt r where r.user.id = :userId and r.message.id in :messageIds")
    Set<Long> findReadMessageIds(@Param("userId") Long userId, @Param("messageIds") Collection<Long> messageIds);

    void deleteByUserId(Long userId);

    void deleteByMessageSenderId(Long senderId);

    void deleteByMessageSessionId(Long sessionId);
}
