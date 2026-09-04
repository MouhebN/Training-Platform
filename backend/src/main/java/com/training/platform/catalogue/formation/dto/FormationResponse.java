package com.training.platform.catalogue.formation.dto;

import com.training.platform.catalogue.category.dto.CategoryResponse;
import com.training.platform.catalogue.formation.entity.FormationLevel;
import com.training.platform.skill.dto.SkillResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

public record FormationResponse(
        Long id,
        String title,
        String description,
        BigDecimal price,
        FormationLevel level,
        Integer durationHours,
        Integer sessionCount,
        Boolean active,
        CategoryResponse category,
        Set<SkillResponse> requiredSkills,
        Instant createdAt,
        Instant updatedAt
) {
}
