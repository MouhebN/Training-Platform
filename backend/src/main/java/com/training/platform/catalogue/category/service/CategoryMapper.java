package com.training.platform.catalogue.category.service;

import com.training.platform.catalogue.category.dto.CategoryResponse;
import com.training.platform.catalogue.category.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
