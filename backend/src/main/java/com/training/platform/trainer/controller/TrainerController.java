package com.training.platform.trainer.controller;

import com.training.platform.common.response.ApiResponse;
import com.training.platform.trainer.dto.TrainerCreateRequest;
import com.training.platform.trainer.dto.TrainerProfileRequest;
import com.training.platform.trainer.dto.TrainerProfileResponse;
import com.training.platform.trainer.service.TrainerService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/trainers")
public class TrainerController {

    private final TrainerService trainerService;

    public TrainerController(TrainerService trainerService) {
        this.trainerService = trainerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TrainerProfileResponse> create(@Valid @RequestBody TrainerCreateRequest request) {
        return ApiResponse.success("Trainer created", trainerService.create(request));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<TrainerProfileResponse>> findAll() {
        return ApiResponse.success("Trainers retrieved", trainerService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<TrainerProfileResponse> findById(@PathVariable Long id) {
        return ApiResponse.success("Trainer retrieved", trainerService.findById(id));
    }

    @GetMapping("/{id}/cv")
    @PreAuthorize("hasRole('ADMIN') or @trainerService.isOwner(#id, authentication.name)")
    public ResponseEntity<Resource> downloadCv(@PathVariable Long id) {
        return trainerService.downloadCv(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TrainerProfileResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TrainerProfileRequest request
    ) {
        return ApiResponse.success("Trainer updated", trainerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        trainerService.delete(id);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('TRAINER')")
    public ApiResponse<TrainerProfileResponse> findMe(Principal principal) {
        return ApiResponse.success("Trainer profile retrieved", trainerService.findMe(principal.getName()));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('TRAINER')")
    public ApiResponse<TrainerProfileResponse> updateMe(
            Principal principal,
            @Valid @RequestBody TrainerProfileRequest request
    ) {
        return ApiResponse.success("Trainer profile updated", trainerService.updateMe(principal.getName(), request));
    }

    @PostMapping(value = "/me/cv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('TRAINER')")
    public ApiResponse<TrainerProfileResponse> uploadCv(
            Principal principal,
            @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.success("CV uploaded", trainerService.uploadCv(principal.getName(), file));
    }

    @GetMapping("/me/cv")
    @PreAuthorize("hasRole('TRAINER')")
    public ResponseEntity<Resource> downloadMyCv(Principal principal) {
        return trainerService.downloadCv(trainerService.findMe(principal.getName()).id());
    }
}
