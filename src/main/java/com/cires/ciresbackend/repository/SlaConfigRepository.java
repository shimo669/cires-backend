package com.cires.ciresbackend.repository;

import com.cires.ciresbackend.entity.GovernmentLevelType;
import com.cires.ciresbackend.entity.SlaConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SlaConfigRepository extends JpaRepository<SlaConfig, Long> {
    Optional<SlaConfig> findByCategoryIdAndLevelType(Long categoryId, GovernmentLevelType levelType);
}
