package com.training.platform.trainer.repository;

import com.training.platform.trainer.entity.TrainerProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainerProfileRepository extends JpaRepository<TrainerProfile, Long> {

    Optional<TrainerProfile> findByUserEmail(String email);

    Optional<TrainerProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
