package com.training.platform.trainer.service;

import com.training.platform.common.exception.BadRequestException;
import com.training.platform.common.exception.ResourceNotFoundException;
import com.training.platform.trainer.dto.TrainerAvailabilityRequest;
import com.training.platform.trainer.dto.TrainerAvailabilityResponse;
import com.training.platform.trainer.entity.TrainerAvailability;
import com.training.platform.trainer.entity.TrainerProfile;
import com.training.platform.trainer.repository.TrainerAvailabilityRepository;
import java.time.DayOfWeek;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrainerAvailabilityService {

    private final TrainerAvailabilityRepository availabilityRepository;
    private final TrainerService trainerService;
    private final TrainerMapper trainerMapper;

    public TrainerAvailabilityService(
            TrainerAvailabilityRepository availabilityRepository,
            TrainerService trainerService,
            TrainerMapper trainerMapper
    ) {
        this.availabilityRepository = availabilityRepository;
        this.trainerService = trainerService;
        this.trainerMapper = trainerMapper;
    }

    @Transactional
    public TrainerAvailabilityResponse createForCurrentTrainer(String email, TrainerAvailabilityRequest request) {
        validateTimeRange(request);
        TrainerProfile trainer = trainerService.getByUserEmail(email);
        return trainerMapper.toAvailabilityResponse(upsertDay(trainer, request));
    }

    @Transactional(readOnly = true)
    public List<TrainerAvailabilityResponse> findByTrainer(Long trainerId) {
        trainerService.getProfile(trainerId);
        return availabilityRepository.findByTrainerIdOrderByDayOfWeekAscStartTimeAsc(trainerId).stream()
                .map(trainerMapper::toAvailabilityResponse)
                .toList();
    }

    @Transactional
    public TrainerAvailabilityResponse update(Long id, TrainerAvailabilityRequest request) {
        validateTimeRange(request);
        TrainerAvailability availability = getAvailability(id);
        assertDayAvailable(availability.getTrainer().getId(), request.dayOfWeek(), availability.getId());
        availability.setDayOfWeek(request.dayOfWeek());
        availability.setStartTime(request.startTime());
        availability.setEndTime(request.endTime());
        return trainerMapper.toAvailabilityResponse(availabilityRepository.save(availability));
    }

    @Transactional
    public void delete(Long id) {
        availabilityRepository.delete(getAvailability(id));
    }

    @Transactional(readOnly = true)
    public boolean isOwner(Long availabilityId, String email) {
        return availabilityRepository.findById(availabilityId)
                .map(availability -> availability.getTrainer().getUser().getEmail().equals(email))
                .orElse(false);
    }

    private TrainerAvailability upsertDay(TrainerProfile trainer, TrainerAvailabilityRequest request) {
        List<TrainerAvailability> existing =
                availabilityRepository.findByTrainerIdAndDayOfWeek(trainer.getId(), request.dayOfWeek());
        if (existing.isEmpty()) {
            return availabilityRepository.save(TrainerAvailability.builder()
                    .trainer(trainer)
                    .dayOfWeek(request.dayOfWeek())
                    .startTime(request.startTime())
                    .endTime(request.endTime())
                    .build());
        }

        TrainerAvailability primary = existing.get(0);
        primary.setStartTime(request.startTime());
        primary.setEndTime(request.endTime());
        if (existing.size() > 1) {
            availabilityRepository.deleteAll(existing.subList(1, existing.size()));
        }
        return availabilityRepository.save(primary);
    }

    private void assertDayAvailable(Long trainerId, DayOfWeek dayOfWeek, Long currentId) {
        availabilityRepository.findByTrainerIdAndDayOfWeek(trainerId, dayOfWeek).stream()
                .filter(slot -> !slot.getId().equals(currentId))
                .findFirst()
                .ifPresent(slot -> {
                    throw new BadRequestException("You already have availability for " + dayOfWeek
                            + ". Update that day instead of creating another slot.");
                });
    }

    private TrainerAvailability getAvailability(Long id) {
        return availabilityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trainer availability not found with id: " + id));
    }

    private void validateTimeRange(TrainerAvailabilityRequest request) {
        if (request.startTime() != null && request.endTime() != null && !request.endTime().isAfter(request.startTime())) {
            throw new BadRequestException("Availability end time must be after start time");
        }
    }
}
