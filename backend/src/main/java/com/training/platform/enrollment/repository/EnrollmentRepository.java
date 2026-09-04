package com.training.platform.enrollment.repository;

import com.training.platform.enrollment.entity.Enrollment;
import com.training.platform.enrollment.entity.EnrollmentStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByLearnerIdAndSessionId(Long learnerId, Long sessionId);

    Optional<Enrollment> findByLearnerIdAndSessionId(Long learnerId, Long sessionId);

    long countBySessionIdAndStatusIn(Long sessionId, Collection<EnrollmentStatus> statuses);

    long countByLearnerIdAndSessionFormationIdAndStatusAndSessionStatusNot(
            Long learnerId,
            Long formationId,
            EnrollmentStatus status,
            com.training.platform.session.entity.SessionStatus sessionStatus
    );

    List<Enrollment> findByLearnerUserEmailOrderByEnrolledAtDesc(String email);

    List<Enrollment> findBySessionIdOrderByEnrolledAtDesc(Long sessionId);

    Optional<Enrollment> findByIdAndLearnerUserEmail(Long id, String email);

    Optional<Enrollment> findFirstBySessionIdAndStatusOrderByEnrolledAtAsc(Long sessionId, EnrollmentStatus status);

    void deleteByLearnerId(Long learnerId);

    void deleteBySessionId(Long sessionId);
}
