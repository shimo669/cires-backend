package com.cires.ciresbackend.repository;

import com.cires.ciresbackend.entity.ReportHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReportHistoryRepository extends JpaRepository<ReportHistory, Long> {
    List<ReportHistory> findByReportId(Long reportId);
    List<ReportHistory> findByActedById(Long userId);
    List<ReportHistory> findByAction(String action);
}

