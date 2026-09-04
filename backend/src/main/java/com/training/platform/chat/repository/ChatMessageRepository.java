package com.training.platform.chat.repository;

import com.training.platform.chat.entity.ChatMessage;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Page<ChatMessage> findBySessionIdOrderByCreatedAtDesc(Long sessionId, Pageable pageable);

    List<ChatMessage> findBySessionIdAndSenderIdNot(Long sessionId, Long senderId);

    @Query("""
            select count(m)
            from ChatMessage m
            where m.session.id = :sessionId
              and m.sender.id <> :userId
              and not exists (
                select r.id from MessageReadReceipt r
                where r.message.id = m.id and r.user.id = :userId
              )
            """)
    long countUnread(@Param("sessionId") Long sessionId, @Param("userId") Long userId);

    void deleteBySenderId(Long senderId);

    void deleteBySessionId(Long sessionId);
}
