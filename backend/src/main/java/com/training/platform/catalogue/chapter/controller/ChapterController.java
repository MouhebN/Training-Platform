package com.training.platform.catalogue.chapter.controller;

import com.training.platform.catalogue.chapter.dto.ChapterRequest;
import com.training.platform.catalogue.chapter.dto.ChapterResponse;
import com.training.platform.catalogue.chapter.service.ChapterService;
import com.training.platform.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChapterController {

    private final ChapterService chapterService;

    public ChapterController(ChapterService chapterService) {
        this.chapterService = chapterService;
    }

    @PostMapping("/api/formations/{formationId}/chapters")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ChapterResponse> create(
            @PathVariable Long formationId,
            @Valid @RequestBody ChapterRequest request
    ) {
        return ApiResponse.success("Chapter created", chapterService.create(formationId, request));
    }

    @GetMapping("/api/formations/{formationId}/chapters")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<ChapterResponse>> findByFormation(@PathVariable Long formationId) {
        return ApiResponse.success("Chapters retrieved", chapterService.findByFormation(formationId));
    }

    @GetMapping("/api/chapters/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ChapterResponse> findById(@PathVariable Long id) {
        return ApiResponse.success("Chapter retrieved", chapterService.findById(id));
    }

    @PutMapping("/api/chapters/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ChapterResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ChapterRequest request
    ) {
        return ApiResponse.success("Chapter updated", chapterService.update(id, request));
    }

    @DeleteMapping("/api/chapters/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        chapterService.delete(id);
    }
}
