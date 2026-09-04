package com.training.platform.planning.controller;

import com.training.platform.common.response.ApiResponse;
import com.training.platform.planning.dto.SessionConflictCheckRequest;
import com.training.platform.planning.dto.SessionConflictCheckResponse;
import com.training.platform.planning.dto.SessionPlanningSuggestionRequest;
import com.training.platform.planning.dto.SessionPlanningSuggestionResponse;
import com.training.platform.planning.dto.TrainerWorkloadResponse;
import com.training.platform.planning.service.SessionPlanningService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessionPlanningController {

    private final SessionPlanningService planningService;

    public SessionPlanningController(SessionPlanningService planningService) {
        this.planningService = planningService;
    }

    @PostMapping("/api/session-planning/suggestions")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<SessionPlanningSuggestionResponse>> suggestions(
            @Valid @RequestBody SessionPlanningSuggestionRequest request
    ) {
        return ApiResponse.success("Planning suggestions retrieved", planningService.suggestions(request));
    }

    @PostMapping("/api/session-planning/conflicts")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SessionConflictCheckResponse> conflicts(@Valid @RequestBody SessionConflictCheckRequest request) {
        return ApiResponse.success("Planning conflicts checked", planningService.checkConflicts(request));
    }

    @GetMapping("/api/trainers/workload")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<TrainerWorkloadResponse>> workload(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to
    ) {
        return ApiResponse.success("Trainer workload retrieved", planningService.workload(from, to));
    }
}
