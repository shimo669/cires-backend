package com.cires.ciresbackend.repository;

import com.cires.ciresbackend.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    Optional<Feedback> findByReportId(Long reportId);
    List<Feedback> findByReportIdIn(List<Long> reportIds);
    List<Feedback> findByCitizenId(Long citizenId);
    List<Feedback> findByRating(Integer rating);
}

