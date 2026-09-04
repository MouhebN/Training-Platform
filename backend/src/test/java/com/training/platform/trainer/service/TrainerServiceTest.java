package com.training.platform.trainer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.training.platform.common.exception.BadRequestException;
import com.training.platform.skill.entity.Skill;
import com.training.platform.skill.service.SkillMapper;
import com.training.platform.skill.service.SkillService;
import com.training.platform.trainer.dto.TrainerCreateRequest;
import com.training.platform.trainer.dto.TrainerProfileResponse;
import com.training.platform.trainer.entity.TrainerProfile;
import com.training.platform.trainer.repository.TrainerAvailabilityRepository;
import com.training.platform.trainer.repository.TrainerProfileRepository;
import com.training.platform.user.entity.Role;
import com.training.platform.user.entity.User;
import com.training.platform.user.repository.UserRepository;
import com.training.platform.user.service.UserMapper;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class TrainerServiceTest {

    @Mock
    private TrainerProfileRepository trainerProfileRepository;

    @Mock
    private TrainerAvailabilityRepository trainerAvailabilityRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SkillService skillService;

    private TrainerService trainerService;

    @BeforeEach
    void setUp() {
        trainerService = new TrainerService(
                trainerProfileRepository,
                trainerAvailabilityRepository,
                userRepository,
                passwordEncoder,
                skillService,
                new TrainerMapper(new UserMapper(), new SkillMapper()),
                org.mockito.Mockito.mock(com.training.platform.common.storage.CvStorageService.class)
        );
    }

    @Test
    void createCreatesTrainerUserAndProfile() {
        Skill java = skill(1L, "Java");
        User savedUser = user(5L, "trainer@test.com", Role.TRAINER);
        TrainerProfile savedProfile = trainerProfile(20L, savedUser, java);
        TrainerCreateRequest request = new TrainerCreateRequest(
                " Trainer ",
                " One ",
                "TRAINER@test.com",
                "password123",
                "22222222",
                "Java trainer",
                "https://example.com/cv.pdf",
                4,
                Set.of(1L)
        );

        when(userRepository.existsByEmail("trainer@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(skillService.getSkills(Set.of(1L))).thenReturn(new LinkedHashSet<>(Set.of(java)));
        when(trainerProfileRepository.save(any(TrainerProfile.class))).thenReturn(savedProfile);

        TrainerProfileResponse response = trainerService.create(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<TrainerProfile> profileCaptor = ArgumentCaptor.forClass(TrainerProfile.class);
        verify(userRepository).save(userCaptor.capture());
        verify(trainerProfileRepository).save(profileCaptor.capture());

        assertThat(userCaptor.getValue().getRole()).isEqualTo(Role.TRAINER);
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("trainer@test.com");
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("encoded");
        assertThat(profileCaptor.getValue().getAverageRating()).isEqualTo(0.0);
        assertThat(profileCaptor.getValue().getActive()).isTrue();
        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.expertise()).hasSize(1);
    }

    @Test
    void createRejectsDuplicateEmail() {
        TrainerCreateRequest request = new TrainerCreateRequest(
                "Trainer",
                "One",
                "trainer@test.com",
                "password123",
                null,
                null,
                null,
                0,
                Set.of()
        );
        when(userRepository.existsByEmail("trainer@test.com")).thenReturn(true);

        assertThatThrownBy(() -> trainerService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Email is already in use");
    }

    private User user(Long id, String email, Role role) {
        return User.builder()
                .id(id)
                .firstName("Trainer")
                .lastName("One")
                .email(email)
                .password("encoded")
                .role(role)
                .enabled(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private TrainerProfile trainerProfile(Long id, User user, Skill skill) {
        return TrainerProfile.builder()
                .id(id)
                .user(user)
                .phone("22222222")
                .bio("Java trainer")
                .cvUrl("https://example.com/cv.pdf")
                .yearsOfExperience(4)
                .expertise(new LinkedHashSet<>(Set.of(skill)))
                .averageRating(0.0)
                .active(true)
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
