package com.training.platform.attendance.controller;

import com.training.platform.attendance.dto.ClassroomAttendanceReportResponse;
import com.training.platform.attendance.dto.ClassroomContextResponse;
import com.training.platform.attendance.service.VirtualAttendanceService;
import com.training.platform.common.response.ApiResponse;
import com.training.platform.session.dto.TrainingSessionResponse;
import com.training.platform.session.service.TrainingSessionService;
import java.security.Principal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VirtualAttendanceController {

    private final VirtualAttendanceService virtualAttendanceService;
    private final TrainingSessionService trainingSessionService;

    public VirtualAttendanceController(
            VirtualAttendanceService virtualAttendanceService,
            TrainingSessionService trainingSessionService
    ) {
        this.virtualAttendanceService = virtualAttendanceService;
        this.trainingSessionService = trainingSessionService;
    }

    @PostMapping("/api/sessions/{id}/classroom/context")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ClassroomContextResponse> context(@PathVariable Long id, Principal principal) {
        return ApiResponse.success("Classroom context retrieved", virtualAttendanceService.getContext(id, principal.getName()));
    }

    @PostMapping("/api/sessions/{id}/classroom/join")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ClassroomContextResponse> join(@PathVariable Long id, Principal principal) {
        return ApiResponse.success("Joined classroom", virtualAttendanceService.join(id, principal.getName()));
    }

    @PostMapping("/api/sessions/{id}/classroom/heartbeat")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> heartbeat(@PathVariable Long id, Principal principal) {
        virtualAttendanceService.heartbeat(id, principal.getName());
        return ApiResponse.success("Heartbeat recorded", null);
    }

    @PostMapping("/api/sessions/{id}/classroom/leave")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> leave(@PathVariable Long id, Principal principal) {
        virtualAttendanceService.leave(id, principal.getName());
        return ApiResponse.success("Left classroom", null);
    }

    @GetMapping("/api/sessions/{id}/classroom/attendance")
    @PreAuthorize("hasRole('ADMIN') or @trainingSessionService.isAssignedTrainer(#id, authentication.name)")
    public ApiResponse<ClassroomAttendanceReportResponse> attendance(@PathVariable Long id, Principal principal) {
        return ApiResponse.success("Classroom attendance retrieved", virtualAttendanceService.getAttendanceReport(id, principal.getName()));
    }

    @PostMapping("/api/sessions/{id}/complete-smart")
    @PreAuthorize("hasRole('ADMIN') or @trainingSessionService.isAssignedTrainer(#id, authentication.name)")
    public ApiResponse<TrainingSessionResponse> completeSmart(@PathVariable Long id, Principal principal) {
        virtualAttendanceService.completeSmart(id, principal.getName());
        return ApiResponse.success("Session completed with smart attendance", trainingSessionService.findById(id));
    }
}
