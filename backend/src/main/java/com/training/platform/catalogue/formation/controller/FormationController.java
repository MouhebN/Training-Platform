package com.training.platform.catalogue.formation.controller;

import com.training.platform.catalogue.formation.dto.FormationRequest;
import com.training.platform.catalogue.formation.dto.FormationResponse;
import com.training.platform.catalogue.formation.entity.FormationLevel;
import com.training.platform.catalogue.formation.service.FormationService;
import com.training.platform.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/formations")
public class FormationController {

    private final FormationService formationService;

    public FormationController(FormationService formationService) {
        this.formationService = formationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<FormationResponse> create(@Valid @RequestBody FormationRequest request) {
        return ApiResponse.success("Formation created", formationService.create(request));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Page<FormationResponse>> findAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) FormationLevel level,
            @RequestParam(required = false) Boolean active,
            Pageable pageable
    ) {
        return ApiResponse.success(
                "Formations retrieved",
                formationService.findAll(keyword, categoryId, level, active, pageable)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<FormationResponse> findById(@PathVariable Long id) {
        return ApiResponse.success("Formation retrieved", formationService.findById(id));
    }

    @GetMapping("/category/{categoryId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<FormationResponse>> findByCategory(@PathVariable Long categoryId) {
        return ApiResponse.success("Formations retrieved", formationService.findByCategory(categoryId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<FormationResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody FormationRequest request
    ) {
        return ApiResponse.success("Formation updated", formationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        formationService.delete(id);
    }
}
