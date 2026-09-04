package com.training.platform.learner.repository;

import com.training.platform.learner.entity.LearnerProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearnerProfileRepository extends JpaRepository<LearnerProfile, Long> {

    Optional<LearnerProfile> findByUserEmail(String email);

    Optional<LearnerProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
