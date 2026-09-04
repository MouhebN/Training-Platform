package com.training.platform.adminintelligence.controller;

import com.training.platform.adminintelligence.dto.AdminIntelligenceDashboardResponse;
import com.training.platform.adminintelligence.service.AdminIntelligenceService;
import com.training.platform.common.response.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/intelligence")
public class AdminIntelligenceController {

    private final AdminIntelligenceService intelligenceService;

    public AdminIntelligenceController(AdminIntelligenceService intelligenceService) {
        this.intelligenceService = intelligenceService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminIntelligenceDashboardResponse> dashboard() {
        return ApiResponse.success("Admin intelligence dashboard generated", intelligenceService.dashboard());
    }
}
