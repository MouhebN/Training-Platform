package com.training.platform.catalogue.chapter.repository;

import com.training.platform.catalogue.chapter.entity.Chapter;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {

    List<Chapter> findByFormationIdOrderByOrderIndexAsc(Long formationId);
}
