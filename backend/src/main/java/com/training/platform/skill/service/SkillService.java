package com.training.platform.skill.service;

import com.training.platform.common.exception.BadRequestException;
import com.training.platform.common.exception.ResourceNotFoundException;
import com.training.platform.skill.dto.SkillRequest;
import com.training.platform.skill.dto.SkillResponse;
import com.training.platform.skill.entity.Skill;
import com.training.platform.skill.repository.SkillRepository;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SkillService {

    private final SkillRepository skillRepository;
    private final SkillMapper skillMapper;

    public SkillService(SkillRepository skillRepository, SkillMapper skillMapper) {
        this.skillRepository = skillRepository;
        this.skillMapper = skillMapper;
    }

    @Transactional
    public SkillResponse create(SkillRequest request) {
        String name = normalizeName(request.name());
        if (skillRepository.existsByNameIgnoreCase(name)) {
            throw new BadRequestException("Skill name is already in use");
        }

        Skill skill = Skill.builder()
                .name(name)
                .description(request.description())
                .build();
        return skillMapper.toResponse(skillRepository.save(skill));
    }

    @Transactional(readOnly = true)
    public List<SkillResponse> findAll() {
        return skillRepository.findAll().stream()
                .map(skillMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SkillResponse findById(Long id) {
        return skillMapper.toResponse(getSkill(id));
    }

    @Transactional
    public SkillResponse update(Long id, SkillRequest request) {
        Skill skill = getSkill(id);
        String name = normalizeName(request.name());

        skillRepository.findByNameIgnoreCase(name)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BadRequestException("Skill name is already in use");
                });

        skill.setName(name);
        skill.setDescription(request.description());
        return skillMapper.toResponse(skillRepository.save(skill));
    }

    @Transactional
    public void delete(Long id) {
        skillRepository.delete(getSkill(id));
    }

    @Transactional(readOnly = true)
    public Set<Skill> getSkills(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new LinkedHashSet<>();
        }
        List<Skill> skills = skillRepository.findByIdIn(ids);
        if (skills.size() != new LinkedHashSet<>(ids).size()) {
            throw new ResourceNotFoundException("One or more skills were not found");
        }
        return new LinkedHashSet<>(skills);
    }

    private Skill getSkill(Long id) {
        return skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found with id: " + id));
    }

    private String normalizeName(String name) {
        return name.trim();
    }
}
