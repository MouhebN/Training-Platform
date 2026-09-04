package com.training.platform.session.controller;

import com.training.platform.common.response.ApiResponse;
import com.training.platform.session.dto.SessionCompletionRequest;
import com.training.platform.session.dto.TrainingSessionRequest;
import com.training.platform.session.dto.TrainingSessionResponse;
import com.training.platform.session.dto.TrainingSessionStatusUpdateRequest;
import com.training.platform.session.dto.TrainingSessionUpdateRequest;
import com.training.platform.session.entity.SessionStatus;
import com.training.platform.session.service.TrainingSessionService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TrainingSessionController {

    private final TrainingSessionService trainingSessionService;

    public TrainingSessionController(TrainingSessionService trainingSessionService) {
        this.trainingSessionService = trainingSessionService;
    }

    @PostMapping("/api/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TrainingSessionResponse> create(@Valid @RequestBody TrainingSessionRequest request) {
        return ApiResponse.success("Training session created", trainingSessionService.create(request));
    }

    @GetMapping("/api/sessions")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Page<TrainingSessionResponse>> findAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long formationId,
            @RequestParam(required = false) Long trainerId,
            @RequestParam(required = false) SessionStatus status,
            @RequestParam(required = false) Boolean online,
            Pageable pageable
    ) {
        return ApiResponse.success(
                "Training sessions retrieved",
                trainingSessionService.findAll(keyword, formationId, trainerId, status, online, pageable)
        );
    }

    @GetMapping("/api/sessions/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<TrainingSessionResponse> findById(@PathVariable Long id) {
        return ApiResponse.success("Training session retrieved", trainingSessionService.findById(id));
    }

    @GetMapping("/api/formations/{formationId}/sessions")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<TrainingSessionResponse>> findByFormation(@PathVariable Long formationId) {
        return ApiResponse.success("Training sessions retrieved", trainingSessionService.findByFormation(formationId));
    }

    @GetMapping("/api/trainers/me/sessions")
    @PreAuthorize("hasRole('TRAINER')")
    public ApiResponse<List<TrainingSessionResponse>> findAssignedToMe(Principal principal) {
        return ApiResponse.success(
                "Trainer sessions retrieved",
                trainingSessionService.findAssignedToTrainer(principal.getName())
        );
    }

    @PutMapping("/api/sessions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TrainingSessionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TrainingSessionUpdateRequest request
    ) {
        return ApiResponse.success("Training session updated", trainingSessionService.update(id, request));
    }

    @PatchMapping("/api/sessions/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TrainingSessionResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody TrainingSessionStatusUpdateRequest request
    ) {
        return ApiResponse.success("Training session status updated", trainingSessionService.updateStatus(id, request.status()));
    }

    @PostMapping("/api/sessions/{id}/complete")
    @PreAuthorize("hasRole('ADMIN') or @trainingSessionService.isAssignedTrainer(#id, authentication.name)")
    public ApiResponse<TrainingSessionResponse> complete(
            @PathVariable Long id,
            @RequestBody(required = false) SessionCompletionRequest request
    ) {
        List<Long> presentEnrollmentIds = request == null || request.presentEnrollmentIds() == null
                ? List.of()
                : request.presentEnrollmentIds();
        return ApiResponse.success("Training session completed", trainingSessionService.complete(id, presentEnrollmentIds));
    }

    @PostMapping("/api/sessions/{id}/start")
    @PreAuthorize("hasRole('ADMIN') or @trainingSessionService.isAssignedTrainer(#id, authentication.name)")
    public ApiResponse<TrainingSessionResponse> start(@PathVariable Long id) {
        return ApiResponse.success("Training session started", trainingSessionService.start(id));
    }

    @PostMapping("/api/sessions/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TrainingSessionResponse> cancel(@PathVariable Long id) {
        return ApiResponse.success("Training session cancelled", trainingSessionService.cancel(id));
    }

    @PostMapping("/api/sessions/{id}/remind")
    @PreAuthorize("hasRole('ADMIN') or @trainingSessionService.isAssignedTrainer(#id, authentication.name)")
    public ApiResponse<TrainingSessionResponse> remind(@PathVariable Long id) {
        return ApiResponse.success("Session reminder sent", trainingSessionService.remind(id));
    }

    @DeleteMapping("/api/sessions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        trainingSessionService.delete(id);
    }
}
