package com.cires.ciresbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportActionService {

    private final ReportService reportService;

    @Transactional
    public void resolveReport(Long reportId, String username) {
        reportService.resolveReport(reportId, username);
    }
}