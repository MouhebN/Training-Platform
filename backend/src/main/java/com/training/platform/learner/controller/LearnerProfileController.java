package com.training.platform.learner.controller;

import com.training.platform.common.response.ApiResponse;
import com.training.platform.learner.dto.ImprovementPlanResponse;
import com.training.platform.learner.dto.LearnerProfileRequest;
import com.training.platform.learner.dto.LearnerProfileResponse;
import com.training.platform.learner.dto.LearnerProfileScoreResponse;
import com.training.platform.learner.dto.LearnerProfileUpdateResponse;
import com.training.platform.learner.dto.SkillGapAnalysisResponse;
import com.training.platform.learner.service.LearnerIntelligenceService;
import com.training.platform.learner.service.LearnerProfileService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/learners")
public class LearnerProfileController {

    private final LearnerProfileService learnerProfileService;
    private final LearnerIntelligenceService learnerIntelligenceService;

    public LearnerProfileController(
            LearnerProfileService learnerProfileService,
            LearnerIntelligenceService learnerIntelligenceService
    ) {
        this.learnerProfileService = learnerProfileService;
        this.learnerIntelligenceService = learnerIntelligenceService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('LEARNER')")
    public ApiResponse<LearnerProfileResponse> findMe(Principal principal) {
        return ApiResponse.success("Learner profile retrieved", learnerProfileService.findMe(principal.getName()));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('LEARNER')")
    public ApiResponse<LearnerProfileUpdateResponse> updateMe(
            Principal principal,
            @Valid @RequestBody LearnerProfileRequest request
    ) {
        return ApiResponse.success("Learner profile updated", learnerProfileService.updateMe(principal.getName(), request));
    }

    @GetMapping("/me/profile-score")
    @PreAuthorize("hasRole('LEARNER')")
    public ApiResponse<LearnerProfileScoreResponse> profileScore(Principal principal) {
        return ApiResponse.success("Learner profile score retrieved", learnerIntelligenceService.profileScore(principal.getName()));
    }

    @GetMapping("/me/skill-gap/{formationId}")
    @PreAuthorize("hasRole('LEARNER')")
    public ApiResponse<SkillGapAnalysisResponse> skillGap(
            Principal principal,
            @PathVariable Long formationId
    ) {
        return ApiResponse.success("Skill gap analysis retrieved", learnerIntelligenceService.skillGap(principal.getName(), formationId));
    }

    @GetMapping("/me/improvement-plan")
    @PreAuthorize("hasRole('LEARNER')")
    public ApiResponse<ImprovementPlanResponse> improvementPlan(Principal principal) {
        return ApiResponse.success("Improvement plan retrieved", learnerIntelligenceService.improvementPlan(principal.getName()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<LearnerProfileResponse>> findAll() {
        return ApiResponse.success("Learners retrieved", learnerProfileService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<LearnerProfileResponse> findById(@PathVariable Long id) {
        return ApiResponse.success("Learner profile retrieved", learnerProfileService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<LearnerProfileResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody LearnerProfileRequest request
    ) {
        return ApiResponse.success("Learner profile updated", learnerProfileService.update(id, request));
    }
}
