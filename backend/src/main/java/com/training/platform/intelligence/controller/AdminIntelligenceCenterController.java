package com.training.platform.intelligence.controller;

import com.training.platform.common.response.ApiResponse;
import com.training.platform.intelligence.dto.AdminIntelligenceResponse;
import com.training.platform.intelligence.service.AdminIntelligenceCenterService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminIntelligenceCenterController {

    private final AdminIntelligenceCenterService intelligenceCenterService;

    public AdminIntelligenceCenterController(AdminIntelligenceCenterService intelligenceCenterService) {
        this.intelligenceCenterService = intelligenceCenterService;
    }

    @GetMapping("/intelligence")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminIntelligenceResponse> getIntelligence() {
        return ApiResponse.success("Admin intelligence center generated", intelligenceCenterService.getIntelligence());
    }
}
