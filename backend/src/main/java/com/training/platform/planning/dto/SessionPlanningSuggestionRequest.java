package com.training.platform.planning.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record SessionPlanningSuggestionRequest(
        @NotNull Long formationId,
        @NotNull LocalDate preferredStartDate,
        @NotNull LocalDate preferredEndDate,
        @NotNull @Min(1) Integer durationHours,
        Boolean online,
        @NotNull @Min(1) Integer preferredCapacity
) {
}
