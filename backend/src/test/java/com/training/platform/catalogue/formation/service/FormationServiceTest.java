package com.training.platform.catalogue.formation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.training.platform.catalogue.category.entity.Category;
import com.training.platform.catalogue.category.service.CategoryMapper;
import com.training.platform.catalogue.category.service.CategoryService;
import com.training.platform.catalogue.formation.dto.FormationRequest;
import com.training.platform.catalogue.formation.dto.FormationResponse;
import com.training.platform.catalogue.formation.entity.Formation;
import com.training.platform.catalogue.formation.entity.FormationLevel;
import com.training.platform.catalogue.formation.repository.FormationRepository;
import com.training.platform.common.exception.ResourceNotFoundException;
import com.training.platform.skill.service.SkillMapper;
import com.training.platform.skill.service.SkillService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class FormationServiceTest {

    @Mock
    private FormationRepository formationRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private SkillService skillService;

    private FormationService formationService;

    @BeforeEach
    void setUp() {
        formationService = new FormationService(
                formationRepository,
                categoryService,
                new FormationMapper(new CategoryMapper(), new SkillMapper()),
                skillService
        );
    }

    @Test
    void createBuildsFormationWithCategory() {
        Category category = category(1L, "IT");
        Formation savedFormation = formation(10L, "Spring Boot Fundamentals", category);
        FormationRequest request = new FormationRequest(
                " Spring Boot Fundamentals ",
                "REST API training",
                BigDecimal.valueOf(250),
                FormationLevel.BEGINNER,
                24,
                4,
                true,
                1L,
                null
        );

        when(categoryService.getCategory(1L)).thenReturn(category);
        when(skillService.getSkills(null)).thenReturn(new java.util.LinkedHashSet<>());
        when(formationRepository.save(any(Formation.class))).thenReturn(savedFormation);

        FormationResponse response = formationService.create(request);

        ArgumentCaptor<Formation> captor = ArgumentCaptor.forClass(Formation.class);
        verify(formationRepository).save(captor.capture());
        Formation formationToSave = captor.getValue();

        assertThat(formationToSave.getTitle()).isEqualTo("Spring Boot Fundamentals");
        assertThat(formationToSave.getCategory()).isEqualTo(category);
        assertThat(formationToSave.getLevel()).isEqualTo(FormationLevel.BEGINNER);
        assertThat(formationToSave.getDurationHours()).isEqualTo(24);
        assertThat(formationToSave.getSessionCount()).isEqualTo(4);
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.category().name()).isEqualTo("IT");
    }

    @Test
    void findAllAppliesFiltersAndReturnsPage() {
        Category category = category(1L, "IT");
        Formation formation = formation(10L, "Spring Boot Fundamentals", category);
        Pageable pageable = PageRequest.of(0, 10);

        when(formationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(formation), pageable, 1));

        Page<FormationResponse> page = formationService.findAll(
                "spring",
                1L,
                FormationLevel.BEGINNER,
                true,
                pageable
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().title()).isEqualTo("Spring Boot Fundamentals");
        verify(formationRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void findByIdThrowsWhenFormationDoesNotExist() {
        when(formationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> formationService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Formation not found with id: 99");
    }

    private Category category(Long id, String name) {
        return Category.builder()
                .id(id)
                .name(name)
                .description(name + " category")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private Formation formation(Long id, String title, Category category) {
        return Formation.builder()
                .id(id)
                .title(title)
                .description("REST API training")
                .price(BigDecimal.valueOf(250))
                .level(FormationLevel.BEGINNER)
                .durationHours(24)
                .sessionCount(1)
                .active(true)
                .category(category)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
