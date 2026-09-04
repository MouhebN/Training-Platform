package com.training.platform.catalogue.chapter.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChapterRequest(
        @NotBlank(message = "Chapter title is required")
        @Size(max = 180, message = "Chapter title must not exceed 180 characters")
        String title,

        String content,

        @NotNull(message = "Order index is required")
        @Min(value = 1, message = "Order index must be greater than or equal to 1")
        Integer orderIndex
) {
}
