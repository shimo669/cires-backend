package com.cires.ciresbackend.repository;

import com.cires.ciresbackend.entity.GovernmentLevelType;
import com.cires.ciresbackend.entity.SlaTimer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SlaTimerRepository extends JpaRepository<SlaTimer, Long> {
    Optional<SlaTimer> findByReportId(Long reportId);
    List<SlaTimer> findByStatus(String status);
    List<SlaTimer> findByLevelType(GovernmentLevelType levelType);
}
