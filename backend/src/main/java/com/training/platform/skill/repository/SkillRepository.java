package com.training.platform.skill.repository;

import com.training.platform.skill.entity.Skill;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    boolean existsByNameIgnoreCase(String name);

    Optional<Skill> findByNameIgnoreCase(String name);

    List<Skill> findByIdIn(Collection<Long> ids);
}
