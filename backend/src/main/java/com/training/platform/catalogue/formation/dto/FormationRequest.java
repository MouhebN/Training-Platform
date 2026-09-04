package com.training.platform.catalogue.formation.dto;

import com.training.platform.catalogue.formation.entity.FormationLevel;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Set;

public record FormationRequest(
        @NotBlank(message = "Formation title is required")
        @Size(max = 180, message = "Formation title must not exceed 180 characters")
        String title,

        String description,

        @DecimalMin(value = "0.00", message = "Price must be greater than or equal to 0")
        BigDecimal price,

        @NotNull(message = "Formation level is required")
        FormationLevel level,

        @NotNull(message = "Duration is required")
        @Min(value = 1, message = "Duration must be greater than 0")
        Integer durationHours,

        @NotNull(message = "Number of sessions is required")
        @Min(value = 1, message = "Number of sessions must be at least 1")
        Integer sessionCount,

        Boolean active,

        @NotNull(message = "Category is required")
        Long categoryId,

        Set<Long> requiredSkillIds
) {
}
