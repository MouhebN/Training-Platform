package com.training.platform.skill.controller;

import com.training.platform.common.response.ApiResponse;
import com.training.platform.skill.dto.SkillRequest;
import com.training.platform.skill.dto.SkillResponse;
import com.training.platform.skill.service.SkillService;
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
@RequestMapping("/api/skills")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SkillResponse> create(@Valid @RequestBody SkillRequest request) {
        return ApiResponse.success("Skill created", skillService.create(request));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<SkillResponse>> findAll() {
        return ApiResponse.success("Skills retrieved", skillService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<SkillResponse> findById(@PathVariable Long id) {
        return ApiResponse.success("Skill retrieved", skillService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SkillResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SkillRequest request
    ) {
        return ApiResponse.success("Skill updated", skillService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        skillService.delete(id);
    }
}
