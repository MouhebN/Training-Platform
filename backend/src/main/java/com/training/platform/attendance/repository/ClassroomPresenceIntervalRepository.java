package com.training.platform.attendance.repository;

import com.training.platform.attendance.entity.ClassroomPresenceInterval;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassroomPresenceIntervalRepository extends JpaRepository<ClassroomPresenceInterval, Long> {

    List<ClassroomPresenceInterval> findBySessionIdOrderByJoinedAtAsc(Long sessionId);

    Optional<ClassroomPresenceInterval> findFirstBySessionIdAndUserIdAndLeftAtIsNull(Long sessionId, Long userId);

    void deleteBySessionId(Long sessionId);
}
