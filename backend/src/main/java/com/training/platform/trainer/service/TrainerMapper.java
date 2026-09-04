package com.training.platform.trainer.service;

import com.training.platform.skill.service.SkillMapper;
import com.training.platform.trainer.dto.TrainerAvailabilityResponse;
import com.training.platform.trainer.dto.TrainerProfileResponse;
import com.training.platform.trainer.entity.TrainerAvailability;
import com.training.platform.trainer.entity.TrainerProfile;
import com.training.platform.user.service.UserMapper;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class TrainerMapper {

    private final UserMapper userMapper;
    private final SkillMapper skillMapper;

    public TrainerMapper(UserMapper userMapper, SkillMapper skillMapper) {
        this.userMapper = userMapper;
        this.skillMapper = skillMapper;
    }

    public TrainerProfileResponse toResponse(TrainerProfile profile) {
        return new TrainerProfileResponse(
                profile.getId(),
                userMapper.toResponse(profile.getUser()),
                profile.getPhone(),
                profile.getBio(),
                profile.getCvUrl(),
                profile.getYearsOfExperience(),
                profile.getExpertise().stream()
                        .sorted(Comparator.comparing(skill -> skill.getName().toLowerCase()))
                        .map(skillMapper::toResponse)
                        .collect(Collectors.toCollection(LinkedHashSet::new)),
                profile.getAverageRating(),
                profile.getActive(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }

    public TrainerAvailabilityResponse toAvailabilityResponse(TrainerAvailability availability) {
        String trainerName = availability.getTrainer().getUser().getFirstName()
                + " "
                + availability.getTrainer().getUser().getLastName();
        return new TrainerAvailabilityResponse(
                availability.getId(),
                availability.getTrainer().getId(),
                trainerName,
                availability.getDayOfWeek(),
                availability.getStartTime(),
                availability.getEndTime(),
                availability.getCreatedAt(),
                availability.getUpdatedAt()
        );
    }
}
