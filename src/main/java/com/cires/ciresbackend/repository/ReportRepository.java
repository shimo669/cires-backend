package com.cires.ciresbackend.repository;

import com.cires.ciresbackend.entity.Report;
import com.cires.ciresbackend.entity.Report.EscalationLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByReporterId(Long reporterId);
    Page<Report> findByReporterId(Long reporterId, Pageable pageable);
    List<Report> findByCurrentEscalationLevel(EscalationLevel level);
    Page<Report> findByCurrentEscalationLevelAndIncidentVillageId(EscalationLevel level, Long villageId, Pageable pageable);
    Page<Report> findByCurrentEscalationLevelAndIncidentVillageCellId(EscalationLevel level, Long cellId, Pageable pageable);
    Page<Report> findByCurrentEscalationLevelAndIncidentVillageCellSectorId(EscalationLevel level, Long sectorId, Pageable pageable);
    Page<Report> findByCurrentEscalationLevelAndIncidentVillageCellSectorDistrictId(EscalationLevel level, Long districtId, Pageable pageable);
    Page<Report> findByCurrentEscalationLevelAndIncidentVillageCellSectorDistrictProvinceId(EscalationLevel level, Long provinceId, Pageable pageable);
    List<Report> findByCategoryId(Long categoryId);
}
