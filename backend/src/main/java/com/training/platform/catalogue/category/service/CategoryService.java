package com.training.platform.catalogue.category.service;

import com.training.platform.catalogue.category.dto.CategoryRequest;
import com.training.platform.catalogue.category.dto.CategoryResponse;
import com.training.platform.catalogue.category.entity.Category;
import com.training.platform.catalogue.category.repository.CategoryRepository;
import com.training.platform.common.exception.BadRequestException;
import com.training.platform.common.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = normalizeName(request.name());
        validateUniqueName(name);

        Category category = Category.builder()
                .name(name)
                .description(request.description())
                .build();

        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll().stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        return categoryMapper.toResponse(getCategory(id));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = getCategory(id);
        String name = normalizeName(request.name());

        categoryRepository.findByNameIgnoreCase(name)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BadRequestException("Category name is already in use");
                });

        category.setName(name);
        category.setDescription(request.description());
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        Category category = getCategory(id);
        categoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    public Category getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    private void validateUniqueName(String name) {
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new BadRequestException("Category name is already in use");
        }
    }

    private String normalizeName(String name) {
        return name.trim();
    }
}
