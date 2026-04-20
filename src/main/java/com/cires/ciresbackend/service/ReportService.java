package com.cires.ciresbackend.service;

import com.cires.ciresbackend.dto.CreateReportRequest;
import com.cires.ciresbackend.dto.ReportConfirmationRequestDTO;
import com.cires.ciresbackend.dto.ReportDTO;
import com.cires.ciresbackend.entity.*;
import com.cires.ciresbackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final VillageRepository villageRepository;
    private final SlaConfigRepository slaConfigRepository;
    private final SlaTimerRepository slaTimerRepository;
    private final ReportHistoryRepository reportHistoryRepository;
    private final FeedbackRepository feedbackRepository;

    @Transactional
    public ReportDTO createReport(CreateReportRequest request, String username) {
        // Get the reporter
        User reporter = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        // Get the category
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        Village incidentVillage = resolveIncidentVillage(request.getIncidentLocationId(), reporter);

        // Create the report
        Report report = new Report();
        report.setTitle(request.getTitle());
        report.setDescription(request.getDescription());
        report.setCategory(category);
        report.setReporter(reporter);

        // Strict Geography Assignment
        report.setIncidentVillage(incidentVillage);
        report.setCurrentEscalationLevel(Report.EscalationLevel.AT_VILLAGE); // Always starts at the Village
        report.setSpecificAddress("N/A"); // Can be updated if request DTO adds specific address

        report.setStatus("PENDING");
        LocalDateTime deadline = resolveSlaDeadline(category.getId(), GovernmentLevelType.VILLAGE, request.getSlaDeadline());
        report.setSlaDeadline(deadline);

        Report savedReport = reportRepository.save(report);

        upsertSlaTimer(savedReport, GovernmentLevelType.VILLAGE, deadline);
        writeHistory(savedReport, reporter, null, GovernmentLevelType.VILLAGE, "CREATED",
                "Report created at village level");

        return convertToDTO(savedReport);
    }

    public List<ReportDTO> getMyReports(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        List<Report> reports = reportRepository.findByReporterId(user.getId());
        return reports.stream().map(this::convertToDTO).toList();
    }

    public List<ReportDTO> getReportsByLevel(String level) {
        if (level == null || level.trim().isEmpty()) {
            throw new IllegalArgumentException("Level is required");
        }

        Report.EscalationLevel escalationLevel = resolveEscalationLevel(level);
        List<Report> reports = reportRepository.findByCurrentEscalationLevel(escalationLevel);

        return reports.stream().map(this::convertToDTO).toList();
    }

    public List<ReportDTO> getAllReports() {
        return reportRepository.findAll().stream().map(this::convertToDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<ReportDTO> getVisibleReportsForUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        return reportRepository.findAll().stream()
                .filter(report -> isVisibleToUser(report, user))
                .map(this::convertToDTO)
                .toList();
    }

    public ReportDTO getReportById(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        return convertToDTO(report);
    }

    private ReportDTO convertToDTO(Report report) {
        ReportDTO dto = new ReportDTO();
        dto.setId(report.getId());
        dto.setTitle(report.getTitle());
        dto.setDescription(report.getDescription());
        dto.setStatus(report.getStatus());
        dto.setCategoryId(report.getCategory().getId());
        dto.setCategoryName(report.getCategory().getCategoryName());
        dto.setReporterId(report.getReporter().getId());
        dto.setReporterUsername(report.getReporter().getUsername());

        // Mapped to the strict village
        dto.setIncidentLocationId(report.getIncidentVillage().getId());
        dto.setIncidentLocationName(report.getIncidentVillage().getName() + " Village");

        dto.setCreatedAt(report.getCreatedAt());
        dto.setSlaDeadline(report.getSlaDeadline());
        applyFeedbackToDTO(report, dto);
        return dto;
    }

    @Transactional
    public void escalateReport(Long reportId, String username) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));
        User actor = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        advanceEscalation(report, actor, false);
    }

    @Transactional
    public void resolveReport(Long reportId, String username) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));
        User actor = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        report.setStatus("PENDING_REPORTER_CONFIRMATION");
        reportRepository.save(report);

        GovernmentLevelType levelType = toGovernmentLevelType(report.getCurrentEscalationLevel());
        slaTimerRepository.findByReportId(reportId).ifPresent(timer -> {
            timer.setStatus(SlaTimer.SlaTimerStatus.COMPLETED);
            timer.setCompletedAt(LocalDateTime.now());
            timer.setBreached(LocalDateTime.now().isAfter(timer.getDeadline()));
            slaTimerRepository.save(timer);
        });

        writeHistory(report, actor, levelType, levelType, "PENDING_REPORTER_CONFIRMATION",
                "Leader marked report as solved and is waiting for reporter confirmation");
    }

    @Transactional
    public void confirmResolution(Long reportId, ReportConfirmationRequestDTO request, String username) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));
        User reporter = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        if (report.getReporter() == null || !report.getReporter().getId().equals(reporter.getId())) {
            throw new RuntimeException("Only the reporter can confirm this report");
        }

        if (!"PENDING_REPORTER_CONFIRMATION".equalsIgnoreCase(report.getStatus())) {
            throw new RuntimeException("Report is not waiting for reporter confirmation");
        }

        if (request.getApproved() == null) {
            throw new RuntimeException("approved flag is required");
        }

        if (Boolean.TRUE.equals(request.getApproved())) {
            validateRating(request.getRating());
            report.setStatus("RESOLVED");
            reportRepository.saveAndFlush(report);

            slaTimerRepository.findByReportId(report.getId()).ifPresent(timer -> {
                timer.setStatus(SlaTimer.SlaTimerStatus.COMPLETED);
                timer.setCompletedAt(LocalDateTime.now());
                timer.setBreached(LocalDateTime.now().isAfter(timer.getDeadline()));
                slaTimerRepository.save(timer);
            });

            saveOrUpdateFeedback(report, reporter, true, request.getRating(), request.getComment());

            GovernmentLevelType levelType = toGovernmentLevelType(report.getCurrentEscalationLevel());
            writeHistory(report, reporter, levelType, levelType, "REPORTER_CONFIRMED",
                    "Reporter confirmed resolution and submitted service rating");
            return;
        }

        // Reporter rejected the fix, so reopen the report at the current level.
        report.setStatus("REOPENED");
        LocalDateTime deadline = resolveSlaDeadline(
                report.getCategory().getId(),
                toGovernmentLevelType(report.getCurrentEscalationLevel()),
                LocalDateTime.now().plusHours(24)
        );
        report.setSlaDeadline(deadline);
        reportRepository.saveAndFlush(report);

        upsertSlaTimer(report, toGovernmentLevelType(report.getCurrentEscalationLevel()), deadline);
        saveOrUpdateFeedback(report, reporter, false, request.getRating(), request.getComment());

        GovernmentLevelType levelType = toGovernmentLevelType(report.getCurrentEscalationLevel());
        writeHistory(report, reporter, levelType, levelType, "REPORTER_REJECTED",
                "Reporter rejected resolution and report has been reopened");
    }

    @Transactional
    public int autoEscalateOverdueReports() {
        List<SlaTimer> overdueTimers = slaTimerRepository
                .findByStatusAndDeadlineBefore(SlaTimer.SlaTimerStatus.ACTIVE, LocalDateTime.now());

        int processed = 0;
        for (SlaTimer timer : overdueTimers) {
            Report report = timer.getReport();
            if (report == null
                    || "RESOLVED".equalsIgnoreCase(report.getStatus())
                    || "PENDING_REPORTER_CONFIRMATION".equalsIgnoreCase(report.getStatus())) {
                continue;
            }

            boolean changed = false;
            while (isReportOverdue(report) && report.getCurrentEscalationLevel() != Report.EscalationLevel.AT_NATIONAL) {
                advanceEscalation(report, null, true);
                changed = true;
            }

            if (isReportOverdue(report) && report.getCurrentEscalationLevel() == Report.EscalationLevel.AT_NATIONAL) {
                markBreached(report, null);
                changed = true;
            }

            if (changed) {
                processed++;
            }
        }

        return processed;
    }

    private Village resolveIncidentVillage(Long incidentLocationId, User reporter) {
        if (incidentLocationId != null) {
            return villageRepository.findById(incidentLocationId)
                    .orElseThrow(() -> new IllegalArgumentException("Incident village not found"));
        }

        if (reporter.getVillage() != null) {
            return reporter.getVillage();
        }

        throw new IllegalArgumentException("Incident location is required (either provide incidentLocationId or assign a village to the reporter)");
    }

    private LocalDateTime resolveSlaDeadline(Long categoryId, GovernmentLevelType levelType, LocalDateTime fallbackDeadline) {
        return slaConfigRepository.findByCategoryIdAndLevelType(categoryId, levelType)
                .map(config -> LocalDateTime.now().plusHours(config.getDurationHours()))
                .orElseGet(() -> fallbackDeadline != null ? fallbackDeadline : LocalDateTime.now().plusHours(24));
    }

    private void upsertSlaTimer(Report report, GovernmentLevelType levelType, LocalDateTime deadline) {
        SlaTimer timer = slaTimerRepository.findByReportId(report.getId()).orElseGet(SlaTimer::new);
        timer.setReport(report);
        timer.setLevelType(levelType);
        timer.setStartTime(LocalDateTime.now());
        timer.setDeadline(deadline);
        timer.setCompletedAt(null);
        timer.setStatus(SlaTimer.SlaTimerStatus.ACTIVE);
        timer.setBreached(false);
        slaTimerRepository.save(timer);
    }

    private void writeHistory(Report report, User actedBy, GovernmentLevelType fromLevel, GovernmentLevelType toLevel,
                              String action, String notes) {
        ReportHistory history = new ReportHistory();
        history.setReport(report);
        history.setActedBy(actedBy);
        history.setFromLevelType(fromLevel);
        history.setToLevelType(toLevel);
        history.setAction(action);
        history.setNotes(notes);
        reportHistoryRepository.save(history);
    }

    private void advanceEscalation(Report report, User actor, boolean automatic) {
        GovernmentLevelType fromLevel = toGovernmentLevelType(report.getCurrentEscalationLevel());

        Report.EscalationLevel newLevel = switch (report.getCurrentEscalationLevel()) {
            case AT_VILLAGE -> Report.EscalationLevel.AT_CELL;
            case AT_CELL -> Report.EscalationLevel.AT_SECTOR;
            case AT_SECTOR -> Report.EscalationLevel.AT_DISTRICT;
            case AT_DISTRICT -> Report.EscalationLevel.AT_PROVINCE;
            case AT_PROVINCE -> Report.EscalationLevel.AT_NATIONAL;
            case AT_NATIONAL -> throw new RuntimeException("Report is already at the highest level (NATIONAL)");
        };

        report.setCurrentEscalationLevel(newLevel);
        report.setStatus("ESCALATED");

        GovernmentLevelType toLevel = toGovernmentLevelType(newLevel);
        LocalDateTime deadline = resolveSlaDeadline(report.getCategory().getId(), toLevel, report.getSlaDeadline());
        report.setSlaDeadline(deadline);
        reportRepository.saveAndFlush(report);

        upsertSlaTimer(report, toLevel, deadline);
        writeHistory(report, actor, fromLevel, toLevel,
                automatic ? "AUTO_ESCALATED" : "ESCALATED",
                automatic ? "Automatically escalated because SLA deadline expired" : "Report escalated to " + toLevel);
    }

    private void markBreached(Report report, User actor) {
        report.setStatus("BREACHED");
        reportRepository.saveAndFlush(report);

        slaTimerRepository.findByReportId(report.getId()).ifPresent(timer -> {
            timer.setStatus(SlaTimer.SlaTimerStatus.BREACHED);
            timer.setCompletedAt(LocalDateTime.now());
            timer.setBreached(true);
            slaTimerRepository.save(timer);
        });

        GovernmentLevelType currentLevel = toGovernmentLevelType(report.getCurrentEscalationLevel());
        writeHistory(report, actor, currentLevel, currentLevel, "BREACHED",
                "Report reached national level and is still overdue");
    }

    private GovernmentLevelType toGovernmentLevelType(Report.EscalationLevel escalationLevel) {
        return switch (escalationLevel) {
            case AT_VILLAGE -> GovernmentLevelType.VILLAGE;
            case AT_CELL -> GovernmentLevelType.CELL;
            case AT_SECTOR -> GovernmentLevelType.SECTOR;
            case AT_DISTRICT -> GovernmentLevelType.DISTRICT;
            case AT_PROVINCE -> GovernmentLevelType.PROVINCE;
            case AT_NATIONAL -> GovernmentLevelType.NATIONAL;
        };
    }

    private boolean isReportOverdue(Report report) {
        return report.getSlaDeadline() != null && LocalDateTime.now().isAfter(report.getSlaDeadline());
    }

    private void applyFeedbackToDTO(Report report, ReportDTO dto) {
        feedbackRepository.findByReportId(report.getId()).ifPresentOrElse(feedback -> {
            dto.setReporterApproved(feedback.getApproved());
            dto.setServiceRating(feedback.getRating());
            dto.setServiceComment(feedback.getComment());
            dto.setReporterConfirmedAt(feedback.getConfirmedAt());
        }, () -> {
            dto.setReporterApproved(null);
            dto.setServiceRating(null);
            dto.setServiceComment(null);
            dto.setReporterConfirmedAt(null);
        });

        dto.setReporterConfirmationRequired("PENDING_REPORTER_CONFIRMATION".equalsIgnoreCase(report.getStatus()));
    }

    private void saveOrUpdateFeedback(Report report, User reporter, boolean approved, Integer rating, String comment) {
        Feedback feedback = feedbackRepository.findByReportId(report.getId()).orElseGet(Feedback::new);
        feedback.setReport(report);
        feedback.setCitizen(reporter);
        feedback.setApproved(approved);
        feedback.setRating(rating);
        feedback.setComment(comment);
        feedback.setConfirmedAt(LocalDateTime.now());
        feedbackRepository.save(feedback);
    }

    private void validateRating(Integer rating) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new RuntimeException("Rating must be between 1 and 5 when approving resolution");
        }
    }

    private Report.EscalationLevel resolveEscalationLevel(String level) {
        String normalized = level.trim().toUpperCase(Locale.ROOT);

        try {
            return Report.EscalationLevel.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            if (!normalized.startsWith("AT_")) {
                normalized = "AT_" + normalized;
            }

            try {
                return Report.EscalationLevel.valueOf(normalized);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid report level");
            }
        }
    }

    private boolean isVisibleToUser(Report report, User user) {
        if (user.getLevelType() == null) {
            return false;
        }

        return switch (user.getLevelType()) {
            case NATIONAL_ADMIN -> true;
            case PROVINCE_GOVERNOR -> matchesProvinceScope(report, user)
                    && report.getCurrentEscalationLevel() == Report.EscalationLevel.AT_PROVINCE;
            case DISTRICT_MAYOR -> matchesDistrictScope(report, user)
                    && report.getCurrentEscalationLevel() == Report.EscalationLevel.AT_DISTRICT;
            case SECTOR_LEADER -> matchesSectorScope(report, user)
                    && report.getCurrentEscalationLevel() == Report.EscalationLevel.AT_SECTOR;
            case CELL_LEADER -> matchesCellScope(report, user)
                    && report.getCurrentEscalationLevel() == Report.EscalationLevel.AT_CELL;
            case VILLAGE_LEADER -> matchesVillageScope(report, user)
                    && report.getCurrentEscalationLevel() == Report.EscalationLevel.AT_VILLAGE;
            case CITIZEN -> report.getReporter() != null && report.getReporter().getId().equals(user.getId());
        };
    }

    private boolean matchesProvinceScope(Report report, User user) {
        return user.getProvince() != null
                && report.getIncidentVillage() != null
                && report.getIncidentVillage().getCell() != null
                && report.getIncidentVillage().getCell().getSector() != null
                && report.getIncidentVillage().getCell().getSector().getDistrict() != null
                && report.getIncidentVillage().getCell().getSector().getDistrict().getProvince() != null
                && report.getIncidentVillage().getCell().getSector().getDistrict().getProvince().getId().equals(user.getProvince().getId());
    }

    private boolean matchesDistrictScope(Report report, User user) {
        return user.getDistrict() != null
                && report.getIncidentVillage() != null
                && report.getIncidentVillage().getCell() != null
                && report.getIncidentVillage().getCell().getSector() != null
                && report.getIncidentVillage().getCell().getSector().getDistrict() != null
                && report.getIncidentVillage().getCell().getSector().getDistrict().getId().equals(user.getDistrict().getId());
    }

    private boolean matchesSectorScope(Report report, User user) {
        return user.getSector() != null
                && report.getIncidentVillage() != null
                && report.getIncidentVillage().getCell() != null
                && report.getIncidentVillage().getCell().getSector() != null
                && report.getIncidentVillage().getCell().getSector().getId().equals(user.getSector().getId());
    }

    private boolean matchesCellScope(Report report, User user) {
        return user.getCell() != null
                && report.getIncidentVillage() != null
                && report.getIncidentVillage().getCell() != null
                && report.getIncidentVillage().getCell().getId().equals(user.getCell().getId());
    }

    private boolean matchesVillageScope(Report report, User user) {
        return user.getVillage() != null
                && report.getIncidentVillage() != null
                && report.getIncidentVillage().getId().equals(user.getVillage().getId());
    }
}