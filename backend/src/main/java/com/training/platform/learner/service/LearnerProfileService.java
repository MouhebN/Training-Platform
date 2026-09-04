package com.training.platform.learner.service;

import com.training.platform.common.exception.BadRequestException;
import com.training.platform.common.exception.ResourceNotFoundException;
import com.training.platform.common.security.JwtService;
import com.training.platform.learner.dto.LearnerProfileRequest;
import com.training.platform.learner.dto.LearnerProfileResponse;
import com.training.platform.learner.dto.LearnerProfileUpdateResponse;
import com.training.platform.learner.entity.LearnerLevel;
import com.training.platform.learner.entity.LearnerProfile;
import com.training.platform.learner.repository.LearnerProfileRepository;
import com.training.platform.skill.service.SkillService;
import com.training.platform.user.entity.User;
import com.training.platform.user.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class LearnerProfileService {

    private final LearnerProfileRepository learnerProfileRepository;
    private final SkillService skillService;
    private final LearnerProfileMapper learnerProfileMapper;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public LearnerProfileService(
            LearnerProfileRepository learnerProfileRepository,
            SkillService skillService,
            LearnerProfileMapper learnerProfileMapper,
            UserRepository userRepository,
            JwtService jwtService
    ) {
        this.learnerProfileRepository = learnerProfileRepository;
        this.skillService = skillService;
        this.learnerProfileMapper = learnerProfileMapper;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public LearnerProfile createEmptyProfile(User user) {
        if (learnerProfileRepository.existsByUserId(user.getId())) {
            return learnerProfileRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Learner profile not found"));
        }
        LearnerProfile profile = LearnerProfile.builder()
                .user(user)
                .currentLevel(LearnerLevel.BEGINNER)
                .build();
        return learnerProfileRepository.save(profile);
    }

    @Transactional(readOnly = true)
    public LearnerProfileResponse findMe(String email) {
        return learnerProfileMapper.toResponse(getByUserEmail(email));
    }

    @Transactional
    public LearnerProfileUpdateResponse updateMe(String email, LearnerProfileRequest request) {
        LearnerProfile profile = getByUserEmail(email);
        applyUpdate(profile, request);
        LearnerProfile saved = learnerProfileRepository.save(profile);
        return new LearnerProfileUpdateResponse(
                learnerProfileMapper.toResponse(saved),
                jwtService.generateToken(toUserDetails(saved.getUser()))
        );
    }

    @Transactional(readOnly = true)
    public List<LearnerProfileResponse> findAll() {
        return learnerProfileRepository.findAll().stream()
                .map(learnerProfileMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LearnerProfileResponse findById(Long id) {
        return learnerProfileMapper.toResponse(getProfile(id));
    }

    @Transactional
    public LearnerProfileResponse update(Long id, LearnerProfileRequest request) {
        LearnerProfile profile = getProfile(id);
        applyUpdate(profile, request);
        return learnerProfileMapper.toResponse(learnerProfileRepository.save(profile));
    }

    private LearnerProfile getProfile(Long id) {
        return learnerProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Learner profile not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public LearnerProfile getByUserEmail(String email) {
        return learnerProfileRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Learner profile not found for current user"));
    }

    private void applyUpdate(LearnerProfile profile, LearnerProfileRequest request) {
        applyEmailUpdate(profile.getUser(), request.email());
        profile.setPhone(request.phone());
        profile.setBio(request.bio());
        profile.setCurrentLevel(request.currentLevel() == null ? LearnerLevel.BEGINNER : request.currentLevel());
        profile.setSkills(skillService.getSkills(request.skillIds()));
        profile.setLearningGoals(request.learningGoals());
    }

    private void applyEmailUpdate(User user, String email) {
        if (!StringUtils.hasText(email)) {
            return;
        }
        String normalized = email.trim().toLowerCase();
        if (normalized.equals(user.getEmail())) {
            return;
        }
        if (userRepository.existsByEmail(normalized)) {
            throw new BadRequestException("Email is already in use");
        }
        user.setEmail(normalized);
        userRepository.save(user);
    }

    private org.springframework.security.core.userdetails.UserDetails toUserDetails(User user) {
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .disabled(!user.isEnabled())
                .accountLocked(Boolean.TRUE.equals(user.getAccountLocked()))
                .build();
    }
}
