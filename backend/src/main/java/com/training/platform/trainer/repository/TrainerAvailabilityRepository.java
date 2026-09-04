package com.training.platform.trainer.repository;

import com.training.platform.trainer.entity.TrainerAvailability;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainerAvailabilityRepository extends JpaRepository<TrainerAvailability, Long> {

    List<TrainerAvailability> findByTrainerIdOrderByDayOfWeekAscStartTimeAsc(Long trainerId);

    List<TrainerAvailability> findByTrainerIdAndDayOfWeek(Long trainerId, DayOfWeek dayOfWeek);

    Optional<TrainerAvailability> findFirstByTrainerIdAndDayOfWeek(Long trainerId, DayOfWeek dayOfWeek);

    void deleteByTrainerId(Long trainerId);
}
