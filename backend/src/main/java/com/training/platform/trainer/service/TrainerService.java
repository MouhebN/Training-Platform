package com.training.platform.trainer.service;

import com.training.platform.common.exception.BadRequestException;
import com.training.platform.common.exception.ResourceNotFoundException;
import com.training.platform.common.storage.CvStorageService;
import com.training.platform.skill.service.SkillService;
import com.training.platform.trainer.dto.TrainerCreateRequest;
import com.training.platform.trainer.dto.TrainerProfileRequest;
import com.training.platform.trainer.dto.TrainerProfileResponse;
import com.training.platform.trainer.entity.TrainerProfile;
import com.training.platform.trainer.repository.TrainerAvailabilityRepository;
import com.training.platform.trainer.repository.TrainerProfileRepository;
import com.training.platform.user.entity.Role;
import com.training.platform.user.entity.User;
import com.training.platform.user.repository.UserRepository;
import java.nio.file.Path;
import java.util.List;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TrainerService {

    private final TrainerProfileRepository trainerProfileRepository;
    private final TrainerAvailabilityRepository trainerAvailabilityRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SkillService skillService;
    private final TrainerMapper trainerMapper;
    private final CvStorageService cvStorageService;

    public TrainerService(
            TrainerProfileRepository trainerProfileRepository,
            TrainerAvailabilityRepository trainerAvailabilityRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            SkillService skillService,
            TrainerMapper trainerMapper,
            CvStorageService cvStorageService
    ) {
        this.trainerProfileRepository = trainerProfileRepository;
        this.trainerAvailabilityRepository = trainerAvailabilityRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.skillService = skillService;
        this.trainerMapper = trainerMapper;
        this.cvStorageService = cvStorageService;
    }

    @Transactional
    public TrainerProfileResponse create(TrainerCreateRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email is already in use");
        }

        User user = User.builder()
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(Role.TRAINER)
                .enabled(true)
                .build();
        User savedUser = userRepository.save(user);

        TrainerProfile profile = TrainerProfile.builder()
                .user(savedUser)
                .phone(request.phone())
                .bio(request.bio())
                .cvUrl(request.cvUrl())
                .yearsOfExperience(request.yearsOfExperience() == null ? 0 : request.yearsOfExperience())
                .expertise(skillService.getSkills(request.expertiseSkillIds()))
                .averageRating(0.0)
                .active(true)
                .build();

        return trainerMapper.toResponse(trainerProfileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public List<TrainerProfileResponse> findAll() {
        return trainerProfileRepository.findAll().stream()
                .map(trainerMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TrainerProfileResponse findById(Long id) {
        return trainerMapper.toResponse(getProfile(id));
    }

    @Transactional
    public TrainerProfileResponse update(Long id, TrainerProfileRequest request) {
        TrainerProfile profile = getProfile(id);
        applyUpdate(profile, request);
        return trainerMapper.toResponse(trainerProfileRepository.save(profile));
    }

    @Transactional
    public void delete(Long id) {
        TrainerProfile profile = getProfile(id);
        trainerAvailabilityRepository.findByTrainerIdOrderByDayOfWeekAscStartTimeAsc(id)
                .forEach(trainerAvailabilityRepository::delete);
        trainerProfileRepository.delete(profile);
        userRepository.delete(profile.getUser());
    }

    @Transactional(readOnly = true)
    public TrainerProfileResponse findMe(String email) {
        return trainerMapper.toResponse(getByUserEmail(email));
    }

    @Transactional
    public TrainerProfileResponse updateMe(String email, TrainerProfileRequest request) {
        TrainerProfile profile = getByUserEmail(email);
        applyUpdate(profile, request);
        return trainerMapper.toResponse(trainerProfileRepository.save(profile));
    }

    @Transactional
    public TrainerProfileResponse uploadCv(String email, MultipartFile file) {
        TrainerProfile profile = getByUserEmail(email);
        String previous = profile.getCvUrl();
        profile.setCvUrl(cvStorageService.store(profile.getId(), file));
        TrainerProfileResponse response = trainerMapper.toResponse(trainerProfileRepository.save(profile));
        cvStorageService.deleteIfStored(previous);
        return response;
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadCv(Long trainerId) {
        TrainerProfile profile = getProfile(trainerId);
        if (!StringUtils.hasText(profile.getCvUrl())) {
            throw new ResourceNotFoundException("This trainer has not uploaded a CV");
        }
        Path path = cvStorageService.resolve(profile.getCvUrl());
        if (path == null) {
            throw new ResourceNotFoundException("CV file was not found");
        }
        return ResponseEntity.ok()
                .contentType(cvStorageService.mediaType(profile.getCvUrl()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + path.getFileName() + "\"")
                .body(new FileSystemResource(path));
    }

    @Transactional(readOnly = true)
    public boolean isOwner(Long trainerId, String email) {
        return trainerProfileRepository.findById(trainerId)
                .map(profile -> profile.getUser().getEmail().equals(email))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public TrainerProfile getProfile(Long id) {
        return trainerProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trainer profile not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public TrainerProfile getByUserEmail(String email) {
        return trainerProfileRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Trainer profile not found for current user"));
    }

    private void applyUpdate(TrainerProfile profile, TrainerProfileRequest request) {
        profile.setPhone(request.phone());
        profile.setBio(request.bio());
        if (StringUtils.hasText(request.cvUrl())) {
            profile.setCvUrl(request.cvUrl());
        }
        profile.setYearsOfExperience(request.yearsOfExperience() == null ? 0 : request.yearsOfExperience());
        profile.setExpertise(skillService.getSkills(request.expertiseSkillIds()));
        profile.setActive(request.active() == null || request.active());
    }
}
