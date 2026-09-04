package com.training.platform.catalogue.chapter.dto;

import java.time.Instant;

public record ChapterResponse(
        Long id,
        String title,
        String content,
        Integer orderIndex,
        Long formationId,
        String formationTitle,
        Instant createdAt,
        Instant updatedAt
) {
}
