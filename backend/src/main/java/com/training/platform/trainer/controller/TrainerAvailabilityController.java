package com.training.platform.trainer.controller;

import com.training.platform.common.response.ApiResponse;
import com.training.platform.trainer.dto.TrainerAvailabilityRequest;
import com.training.platform.trainer.dto.TrainerAvailabilityResponse;
import com.training.platform.trainer.service.TrainerAvailabilityService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TrainerAvailabilityController {

    private final TrainerAvailabilityService availabilityService;

    public TrainerAvailabilityController(TrainerAvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @PostMapping("/api/trainers/me/availability")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TRAINER')")
    public ApiResponse<TrainerAvailabilityResponse> create(
            Principal principal,
            @Valid @RequestBody TrainerAvailabilityRequest request
    ) {
        return ApiResponse.success(
                "Trainer availability created",
                availabilityService.createForCurrentTrainer(principal.getName(), request)
        );
    }

    @GetMapping("/api/trainers/{trainerId}/availability")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<TrainerAvailabilityResponse>> findByTrainer(@PathVariable Long trainerId) {
        return ApiResponse.success("Trainer availability retrieved", availabilityService.findByTrainer(trainerId));
    }

    @PutMapping("/api/trainer-availability/{id}")
    @PreAuthorize("hasRole('ADMIN') or @trainerAvailabilityService.isOwner(#id, authentication.name)")
    public ApiResponse<TrainerAvailabilityResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TrainerAvailabilityRequest request
    ) {
        return ApiResponse.success("Trainer availability updated", availabilityService.update(id, request));
    }

    @DeleteMapping("/api/trainer-availability/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN') or @trainerAvailabilityService.isOwner(#id, authentication.name)")
    public void delete(@PathVariable Long id) {
        availabilityService.delete(id);
    }
}
