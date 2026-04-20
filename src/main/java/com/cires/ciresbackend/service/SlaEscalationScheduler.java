package com.cires.ciresbackend.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SlaEscalationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(SlaEscalationScheduler.class);

    private final ReportService reportService;

    @Scheduled(fixedDelayString = "${sla.escalation.delay-ms:60000}")
    public void escalateOverdueSlaTimers() {
        int processed = reportService.autoEscalateOverdueReports();
        if (processed > 0) {
            logger.info("Auto-escalated {} overdue report(s)", processed);
        }
    }
}

