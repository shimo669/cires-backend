package com.cires.ciresbackend.repository;

import com.cires.ciresbackend.entity.Report;
import com.cires.ciresbackend.entity.Report.EscalationLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByReporterId(Long reporterId);
    List<Report> findByCurrentEscalationLevel(EscalationLevel level);
    List<Report> findByCategoryId(Long categoryId);
}
