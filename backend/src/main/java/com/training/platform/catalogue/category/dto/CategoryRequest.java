package com.training.platform.catalogue.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "Category name is required")
        @Size(max = 120, message = "Category name must not exceed 120 characters")
        String name,

        String description
) {
}
