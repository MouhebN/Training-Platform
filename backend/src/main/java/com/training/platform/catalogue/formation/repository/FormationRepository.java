package com.training.platform.catalogue.formation.repository;

import com.training.platform.catalogue.formation.entity.Formation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FormationRepository extends JpaRepository<Formation, Long>, JpaSpecificationExecutor<Formation> {

    List<Formation> findByCategoryId(Long categoryId);

    Optional<Formation> findByTitleIgnoreCase(String title);

    List<Formation> findTop20ByActiveTrueOrderByCreatedAtDesc();

    List<Formation> findByActiveTrue();
}
