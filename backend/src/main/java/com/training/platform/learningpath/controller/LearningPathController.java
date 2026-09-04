package com.training.platform.learningpath.controller;

import com.training.platform.common.response.ApiResponse;
import com.training.platform.learningpath.dto.LearningPathResponse;
import com.training.platform.learningpath.service.LearningPathService;
import java.security.Principal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/learning-path")
public class LearningPathController {

    private final LearningPathService learningPathService;

    public LearningPathController(LearningPathService learningPathService) {
        this.learningPathService = learningPathService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('LEARNER')")
    public ApiResponse<LearningPathResponse> myLearningPath(Principal principal) {
        return ApiResponse.success("Learning path generated", learningPathService.generate(principal.getName()));
    }
}
