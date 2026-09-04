package com.training.platform.catalogue.formation.service;

import com.training.platform.catalogue.formation.entity.Formation;
import com.training.platform.catalogue.formation.entity.FormationLevel;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class FormationSpecifications {

    private FormationSpecifications() {
    }

    public static Specification<Formation> withFilters(
            String keyword,
            Long categoryId,
            FormationLevel level,
            Boolean active
    ) {
        return Specification.where(keywordContains(keyword))
                .and(categoryIdEquals(categoryId))
                .and(levelEquals(level))
                .and(activeEquals(active));
    }

    private static Specification<Formation> keywordContains(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(keyword)) {
                return criteriaBuilder.conjunction();
            }
            String likeKeyword = "%" + keyword.trim().toLowerCase() + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), likeKeyword),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), likeKeyword)
            );
        };
    }

    private static Specification<Formation> categoryIdEquals(Long categoryId) {
        return (root, query, criteriaBuilder) -> categoryId == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.get("category").get("id"), categoryId);
    }

    private static Specification<Formation> levelEquals(FormationLevel level) {
        return (root, query, criteriaBuilder) -> level == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.get("level"), level);
    }

    private static Specification<Formation> activeEquals(Boolean active) {
        return (root, query, criteriaBuilder) -> active == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.get("active"), active);
    }
}
