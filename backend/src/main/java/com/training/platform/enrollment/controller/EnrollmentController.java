package com.training.platform.enrollment.controller;

import com.training.platform.common.response.ApiResponse;
import com.training.platform.enrollment.dto.EnrollmentCancelResponse;
import com.training.platform.enrollment.dto.EnrollmentResponse;
import com.training.platform.enrollment.dto.EnrollmentStatusUpdateRequest;
import com.training.platform.enrollment.service.EnrollmentService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @PostMapping("/api/sessions/{sessionId}/enroll")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('LEARNER')")
    public ApiResponse<EnrollmentResponse> enroll(@PathVariable Long sessionId, Principal principal) {
        return ApiResponse.success("Enrollment created", enrollmentService.enroll(sessionId, principal.getName()));
    }

    @GetMapping("/api/enrollments/me")
    @PreAuthorize("hasRole('LEARNER')")
    public ApiResponse<List<EnrollmentResponse>> findMine(Principal principal) {
        return ApiResponse.success("Enrollments retrieved", enrollmentService.findMine(principal.getName()));
    }

    @GetMapping("/api/sessions/{sessionId}/enrollments")
    @PreAuthorize("hasRole('ADMIN') or @trainingSessionService.isAssignedTrainer(#sessionId, authentication.name)")
    public ApiResponse<List<EnrollmentResponse>> findBySession(@PathVariable Long sessionId) {
        return ApiResponse.success("Session enrollments retrieved", enrollmentService.findBySession(sessionId));
    }

    @GetMapping("/api/enrollments/{id}")
    @PreAuthorize("hasRole('ADMIN') or @enrollmentService.isOwnerLearner(#id, authentication.name) or @enrollmentService.isAssignedTrainerForEnrollment(#id, authentication.name)")
    public ApiResponse<EnrollmentResponse> findById(@PathVariable Long id) {
        return ApiResponse.success("Enrollment retrieved", enrollmentService.findById(id));
    }

    @PatchMapping("/api/enrollments/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<EnrollmentResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody EnrollmentStatusUpdateRequest request
    ) {
        return ApiResponse.success("Enrollment status updated", enrollmentService.updateStatus(id, request.status()));
    }

    @PatchMapping("/api/enrollments/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN') or @enrollmentService.isOwnerLearner(#id, authentication.name)")
    public ApiResponse<EnrollmentCancelResponse> cancel(@PathVariable Long id) {
        return ApiResponse.success("Enrollment cancelled", enrollmentService.cancel(id));
    }

    @DeleteMapping("/api/enrollments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN') or @enrollmentService.isOwnerLearner(#id, authentication.name)")
    public void delete(@PathVariable Long id) {
        enrollmentService.delete(id);
    }
}
