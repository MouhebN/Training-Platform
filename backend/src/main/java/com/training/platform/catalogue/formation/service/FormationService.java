package com.training.platform.catalogue.formation.service;

import com.training.platform.catalogue.category.entity.Category;
import com.training.platform.catalogue.category.service.CategoryService;
import com.training.platform.catalogue.formation.dto.FormationRequest;
import com.training.platform.catalogue.formation.dto.FormationResponse;
import com.training.platform.catalogue.formation.entity.Formation;
import com.training.platform.catalogue.formation.entity.FormationLevel;
import com.training.platform.catalogue.formation.repository.FormationRepository;
import com.training.platform.common.exception.ResourceNotFoundException;
import com.training.platform.skill.service.SkillService;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FormationService {

    private final FormationRepository formationRepository;
    private final CategoryService categoryService;
    private final FormationMapper formationMapper;
    private final SkillService skillService;

    public FormationService(
            FormationRepository formationRepository,
            CategoryService categoryService,
            FormationMapper formationMapper,
            SkillService skillService
    ) {
        this.formationRepository = formationRepository;
        this.categoryService = categoryService;
        this.formationMapper = formationMapper;
        this.skillService = skillService;
    }

    @Transactional
    public FormationResponse create(FormationRequest request) {
        Category category = categoryService.getCategory(request.categoryId());
        Formation formation = Formation.builder()
                .title(request.title().trim())
                .description(request.description())
                .price(request.price())
                .level(request.level())
                .durationHours(request.durationHours())
                .sessionCount(request.sessionCount())
                .active(request.active() == null || request.active())
                .category(category)
                .requiredSkills(skillService.getSkills(request.requiredSkillIds()))
                .build();

        return formationMapper.toResponse(formationRepository.save(formation));
    }

    @Transactional(readOnly = true)
    public Page<FormationResponse> findAll(
            String keyword,
            Long categoryId,
            FormationLevel level,
            Boolean active,
            Pageable pageable
    ) {
        return formationRepository.findAll(
                        FormationSpecifications.withFilters(keyword, categoryId, level, active),
                        pageable
                )
                .map(formationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public FormationResponse findById(Long id) {
        return formationMapper.toResponse(getFormation(id));
    }

    @Transactional(readOnly = true)
    public List<FormationResponse> findByCategory(Long categoryId) {
        categoryService.getCategory(categoryId);
        return formationRepository.findByCategoryId(categoryId).stream()
                .map(formationMapper::toResponse)
                .toList();
    }

    @Transactional
    public FormationResponse update(Long id, FormationRequest request) {
        Formation formation = getFormation(id);
        Category category = categoryService.getCategory(request.categoryId());

        formation.setTitle(request.title().trim());
        formation.setDescription(request.description());
        formation.setPrice(request.price());
        formation.setLevel(request.level());
        formation.setDurationHours(request.durationHours());
        formation.setSessionCount(request.sessionCount());
        formation.setActive(request.active() == null || request.active());
        formation.setCategory(category);
        formation.setRequiredSkills(skillService.getSkills(request.requiredSkillIds()));

        return formationMapper.toResponse(formationRepository.save(formation));
    }

    @Transactional
    public void delete(Long id) {
        Formation formation = getFormation(id);
        formationRepository.delete(formation);
    }

    @Transactional(readOnly = true)
    public Formation getFormation(Long id) {
        return formationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Formation not found with id: " + id));
    }
}
