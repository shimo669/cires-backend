package com.cires.ciresbackend.service;

import com.cires.ciresbackend.entity.Report;
import com.cires.ciresbackend.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportActionService {

    private final ReportRepository reportRepository;

    @Transactional
    public void resolveReport(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));
        report.setStatus("RESOLVED"); // Changed from ReportStatus Enum to String
        reportRepository.save(report);
    }
}