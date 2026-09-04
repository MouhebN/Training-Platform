package com.training.platform.catalogue.chapter.service;

import com.training.platform.catalogue.chapter.dto.ChapterResponse;
import com.training.platform.catalogue.chapter.entity.Chapter;
import org.springframework.stereotype.Component;

@Component
public class ChapterMapper {

    public ChapterResponse toResponse(Chapter chapter) {
        return new ChapterResponse(
                chapter.getId(),
                chapter.getTitle(),
                chapter.getContent(),
                chapter.getOrderIndex(),
                chapter.getFormation().getId(),
                chapter.getFormation().getTitle(),
                chapter.getCreatedAt(),
                chapter.getUpdatedAt()
        );
    }
}
