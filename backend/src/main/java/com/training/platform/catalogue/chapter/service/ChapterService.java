package com.training.platform.catalogue.chapter.service;

import com.training.platform.catalogue.chapter.dto.ChapterRequest;
import com.training.platform.catalogue.chapter.dto.ChapterResponse;
import com.training.platform.catalogue.chapter.entity.Chapter;
import com.training.platform.catalogue.chapter.repository.ChapterRepository;
import com.training.platform.catalogue.formation.entity.Formation;
import com.training.platform.catalogue.formation.service.FormationService;
import com.training.platform.common.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChapterService {

    private final ChapterRepository chapterRepository;
    private final FormationService formationService;
    private final ChapterMapper chapterMapper;

    public ChapterService(
            ChapterRepository chapterRepository,
            FormationService formationService,
            ChapterMapper chapterMapper
    ) {
        this.chapterRepository = chapterRepository;
        this.formationService = formationService;
        this.chapterMapper = chapterMapper;
    }

    @Transactional
    public ChapterResponse create(Long formationId, ChapterRequest request) {
        Formation formation = formationService.getFormation(formationId);
        Chapter chapter = Chapter.builder()
                .title(request.title().trim())
                .content(request.content())
                .orderIndex(request.orderIndex())
                .formation(formation)
                .build();

        return chapterMapper.toResponse(chapterRepository.save(chapter));
    }

    @Transactional(readOnly = true)
    public List<ChapterResponse> findByFormation(Long formationId) {
        formationService.getFormation(formationId);
        return chapterRepository.findByFormationIdOrderByOrderIndexAsc(formationId).stream()
                .map(chapterMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ChapterResponse findById(Long id) {
        return chapterMapper.toResponse(getChapter(id));
    }

    @Transactional
    public ChapterResponse update(Long id, ChapterRequest request) {
        Chapter chapter = getChapter(id);
        chapter.setTitle(request.title().trim());
        chapter.setContent(request.content());
        chapter.setOrderIndex(request.orderIndex());

        return chapterMapper.toResponse(chapterRepository.save(chapter));
    }

    @Transactional
    public void delete(Long id) {
        Chapter chapter = getChapter(id);
        chapterRepository.delete(chapter);
    }

    private Chapter getChapter(Long id) {
        return chapterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found with id: " + id));
    }
}
