package com.cires.ciresbackend.repository;

import com.cires.ciresbackend.entity.SlaTimer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SlaTimerRepository extends JpaRepository<SlaTimer, Long> {
    Optional<SlaTimer> findByReportId(Long reportId);
    List<SlaTimer> findByStatusAndDeadlineBefore(SlaTimer.SlaTimerStatus status, LocalDateTime deadline);
    List<SlaTimer> findByAutoFixedBySchedulerTrueOrderByAutoFixedAtDesc(Pageable pageable);
}
