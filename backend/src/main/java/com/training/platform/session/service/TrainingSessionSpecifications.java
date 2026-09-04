package com.training.platform.session.service;

import com.training.platform.session.entity.SessionStatus;
import com.training.platform.session.entity.TrainingSession;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class TrainingSessionSpecifications {

    private TrainingSessionSpecifications() {
    }

    public static Specification<TrainingSession> withFilters(
            String keyword,
            Long formationId,
            Long trainerId,
            SessionStatus status,
            Boolean online
    ) {
        return Specification.where(keywordContains(keyword))
                .and(formationIdEquals(formationId))
                .and(trainerIdEquals(trainerId))
                .and(statusEquals(status))
                .and(onlineEquals(online));
    }

    private static Specification<TrainingSession> keywordContains(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(keyword)) {
                return criteriaBuilder.conjunction();
            }
            String likeKeyword = "%" + keyword.trim().toLowerCase() + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), likeKeyword),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), likeKeyword),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("location")), likeKeyword)
            );
        };
    }

    private static Specification<TrainingSession> formationIdEquals(Long formationId) {
        return (root, query, criteriaBuilder) -> formationId == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.get("formation").get("id"), formationId);
    }

    private static Specification<TrainingSession> trainerIdEquals(Long trainerId) {
        return (root, query, criteriaBuilder) -> trainerId == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.get("trainer").get("id"), trainerId);
    }

    private static Specification<TrainingSession> statusEquals(SessionStatus status) {
        return (root, query, criteriaBuilder) -> status == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.get("status"), status);
    }

    private static Specification<TrainingSession> onlineEquals(Boolean online) {
        return (root, query, criteriaBuilder) -> online == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.get("online"), online);
    }
}
