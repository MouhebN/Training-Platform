package com.training.platform.learner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.training.platform.common.security.JwtService;
import com.training.platform.learner.dto.LearnerProfileRequest;
import com.training.platform.learner.dto.LearnerProfileUpdateResponse;
import com.training.platform.learner.entity.LearnerLevel;
import com.training.platform.learner.entity.LearnerProfile;
import com.training.platform.learner.repository.LearnerProfileRepository;
import com.training.platform.skill.entity.Skill;
import com.training.platform.skill.service.SkillMapper;
import com.training.platform.skill.service.SkillService;
import com.training.platform.user.entity.Role;
import com.training.platform.user.entity.User;
import com.training.platform.user.repository.UserRepository;
import com.training.platform.user.service.UserMapper;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LearnerProfileServiceTest {

    @Mock
    private LearnerProfileRepository learnerProfileRepository;

    @Mock
    private SkillService skillService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    private LearnerProfileService learnerProfileService;

    @BeforeEach
    void setUp() {
        learnerProfileService = new LearnerProfileService(
                learnerProfileRepository,
                skillService,
                new LearnerProfileMapper(new UserMapper(), new SkillMapper()),
                userRepository,
                jwtService
        );
    }

    @Test
    void createEmptyProfileCreatesBeginnerProfileForUser() {
        User user = user(1L, "learner@test.com", Role.LEARNER);
        LearnerProfile savedProfile = profile(10L, user);

        when(learnerProfileRepository.existsByUserId(1L)).thenReturn(false);
        when(learnerProfileRepository.save(any(LearnerProfile.class))).thenReturn(savedProfile);

        LearnerProfile result = learnerProfileService.createEmptyProfile(user);

        ArgumentCaptor<LearnerProfile> captor = ArgumentCaptor.forClass(LearnerProfile.class);
        verify(learnerProfileRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        assertThat(captor.getValue().getCurrentLevel()).isEqualTo(LearnerLevel.BEGINNER);
        assertThat(result.getId()).isEqualTo(10L);
    }

    @Test
    void updateMeUpdatesProfileSkillsAndGoals() {
        User user = user(1L, "learner@test.com", Role.LEARNER);
        LearnerProfile profile = profile(10L, user);
        Skill java = skill(1L, "Java");
        Skill spring = skill(2L, "Spring Boot");
        LearnerProfileRequest request = new LearnerProfileRequest(
                "learner.new@test.com",
                "22222222",
                "Backend learner",
                LearnerLevel.INTERMEDIATE,
                Set.of(1L, 2L),
                "Become a backend developer"
        );

        when(learnerProfileRepository.findByUserEmail("learner@test.com")).thenReturn(Optional.of(profile));
        when(userRepository.existsByEmail("learner.new@test.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(skillService.getSkills(Set.of(1L, 2L))).thenReturn(new LinkedHashSet<>(Set.of(java, spring)));
        when(learnerProfileRepository.save(any(LearnerProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any())).thenReturn("new-token");

        LearnerProfileUpdateResponse response = learnerProfileService.updateMe("learner@test.com", request);

        assertThat(response.token()).isEqualTo("new-token");
        assertThat(response.profile().user().email()).isEqualTo("learner.new@test.com");
        assertThat(response.profile().phone()).isEqualTo("22222222");
        assertThat(response.profile().currentLevel()).isEqualTo(LearnerLevel.INTERMEDIATE);
        assertThat(response.profile().skills()).hasSize(2);
        assertThat(response.profile().learningGoals()).isEqualTo("Become a backend developer");
    }

    private LearnerProfile profile(Long id, User user) {
        return LearnerProfile.builder()
                .id(id)
                .user(user)
                .currentLevel(LearnerLevel.BEGINNER)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private User user(Long id, String email, Role role) {
        return User.builder()
                .id(id)
                .firstName("Test")
                .lastName("User")
                .email(email)
                .password("encoded")
                .role(role)
                .enabled(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private Skill skill(Long id, String name) {
        return Skill.builder()
                .id(id)
                .name(name)
                .description(name)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
