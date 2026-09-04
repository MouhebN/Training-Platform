package com.training.platform.catalogue.formation.service;

import com.training.platform.catalogue.category.service.CategoryMapper;
import com.training.platform.catalogue.formation.dto.FormationResponse;
import com.training.platform.catalogue.formation.entity.Formation;
import com.training.platform.skill.service.SkillMapper;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class FormationMapper {

    private final CategoryMapper categoryMapper;
    private final SkillMapper skillMapper;

    public FormationMapper(CategoryMapper categoryMapper, SkillMapper skillMapper) {
        this.categoryMapper = categoryMapper;
        this.skillMapper = skillMapper;
    }

    public FormationResponse toResponse(Formation formation) {
        return new FormationResponse(
                formation.getId(),
                formation.getTitle(),
                formation.getDescription(),
                formation.getPrice(),
                formation.getLevel(),
                formation.getDurationHours(),
                formation.getSessionCount(),
                formation.getActive(),
                categoryMapper.toResponse(formation.getCategory()),
                formation.getRequiredSkills().stream()
                        .sorted(Comparator.comparing(skill -> skill.getName().toLowerCase()))
                        .map(skillMapper::toResponse)
                        .collect(Collectors.toCollection(LinkedHashSet::new)),
                formation.getCreatedAt(),
                formation.getUpdatedAt()
        );
    }
}
