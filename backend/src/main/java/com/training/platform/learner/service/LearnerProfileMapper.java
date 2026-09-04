package com.training.platform.learner.service;

import com.training.platform.learner.dto.LearnerProfileResponse;
import com.training.platform.learner.entity.LearnerProfile;
import com.training.platform.skill.service.SkillMapper;
import com.training.platform.user.service.UserMapper;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class LearnerProfileMapper {

    private final UserMapper userMapper;
    private final SkillMapper skillMapper;

    public LearnerProfileMapper(UserMapper userMapper, SkillMapper skillMapper) {
        this.userMapper = userMapper;
        this.skillMapper = skillMapper;
    }

    public LearnerProfileResponse toResponse(LearnerProfile profile) {
        return new LearnerProfileResponse(
                profile.getId(),
                userMapper.toResponse(profile.getUser()),
                profile.getPhone(),
                profile.getBio(),
                profile.getCurrentLevel(),
                profile.getSkills().stream()
                        .sorted(Comparator.comparing(skill -> skill.getName().toLowerCase()))
                        .map(skillMapper::toResponse)
                        .collect(Collectors.toCollection(LinkedHashSet::new)),
                profile.getLearningGoals(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
