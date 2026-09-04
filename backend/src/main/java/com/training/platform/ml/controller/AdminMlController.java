package com.training.platform.ml.controller;

import com.training.platform.common.exception.BadRequestException;
import com.training.platform.common.response.ApiResponse;
import com.training.platform.ml.service.MlSuggestionClient;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/ml")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMlController {

    private final MlSuggestionClient mlSuggestionClient;

    public AdminMlController(MlSuggestionClient mlSuggestionClient) {
        this.mlSuggestionClient = mlSuggestionClient;
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.success("MLA health", safeCall(mlSuggestionClient::getHealth));
    }

    @GetMapping("/pipeline")
    public ApiResponse<Map<String, Object>> pipeline() {
        return ApiResponse.success("MLA pipeline", safeCall(mlSuggestionClient::getPipeline));
    }

    @GetMapping("/metrics")
    public ApiResponse<Map<String, Object>> metrics() {
        return ApiResponse.success("MLA metrics", safeCall(mlSuggestionClient::getMetrics));
    }

    @GetMapping("/dataset-sample")
    public ApiResponse<Map<String, Object>> datasetSample(@RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.success("MLA dataset sample", safeCall(() -> mlSuggestionClient.getDatasetSample(limit)));
    }

    @PostMapping("/retrain")
    public ApiResponse<Map<String, Object>> retrain() {
        if (!mlSuggestionClient.isEnabled()) {
            throw new BadRequestException("MLA integration is disabled");
        }
        try {
            return ApiResponse.success("MLA model retrained", mlSuggestionClient.retrain());
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), ex);
        }
    }

    private Map<String, Object> safeCall(java.util.function.Supplier<Map<String, Object>> supplier) {
        if (!mlSuggestionClient.isEnabled()) {
            throw new BadRequestException("MLA integration is disabled");
        }
        try {
            return supplier.get();
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), ex);
        }
    }
}
