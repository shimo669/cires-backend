package com.cires.ciresbackend.service;

import com.cires.ciresbackend.dto.CreateReportRequest;
import com.cires.ciresbackend.dto.ReportConfirmationRequestDTO;
import com.cires.ciresbackend.dto.ReportDTO;
import com.cires.ciresbackend.entity.*;
import com.cires.ciresbackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

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
        logger.info("Attempting to create report for user: {} with categoryId: {} and locationId: {}", 
                username, request.getCategoryId(), request.getIncidentLocationId());
        
        // Get the reporter
        User reporter = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        logger.info("Found reporter: {}", reporter.getUsername());

        // Get the category
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + request.getCategoryId()));
        logger.info("Found category: {}", category.getCategoryName());

        Village incidentVillage = resolveIncidentVillage(request.getIncidentLocationId(), reporter);
        logger.info("Resolved incident village: {}", incidentVillage.getName());

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
        logger.info("Calculated SLA deadline: {}", deadline);

        Report savedReport = reportRepository.save(report);
        logger.info("Report saved with ID: {}", savedReport.getId());

        upsertSlaTimer(savedReport, GovernmentLevelType.VILLAGE, deadline);
        logger.info("SLA Timer upserted");

        writeHistory(savedReport, reporter, null, GovernmentLevelType.VILLAGE, "CREATED",
                "Report created at village level");
        logger.info("History record written");

        ReportDTO dto = convertToDTO(savedReport);
        logger.info("Converted to DTO successfully");
        
        return dto;
    }

    public List<ReportDTO> getMyReports(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        List<Report> reports = reportRepository.findByReporterId(user.getId());
        return convertReportsToDTOWithFeedback(reports);
    }

    public List<ReportDTO> getReportsByLevel(String level) {
        if (level == null || level.trim().isEmpty()) {
            throw new IllegalArgumentException("Level is required");
        }

        Report.EscalationLevel escalationLevel = resolveEscalationLevel(level);
        List<Report> reports = reportRepository.findByCurrentEscalationLevel(escalationLevel);

        return convertReportsToDTOWithFeedback(reports);
    }

    public List<ReportDTO> getAllReports() {
        return convertReportsToDTOWithFeedback(reportRepository.findAll());
    }

    @Transactional(readOnly = true)
    public List<ReportDTO> getVisibleReportsForUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        List<Report> visibleReports = findVisibleReportsPage(user, Pageable.unpaged()).getContent();

        return convertReportsToDTOWithFeedback(visibleReports);
    }

    @Transactional(readOnly = true)
    public Page<ReportDTO> getVisibleReportsPageForUser(String username, int page, int size) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 200));
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Report> reportPage = findVisibleReportsPage(user, pageable);
        return convertReportPageToDTOPage(reportPage);
    }

    public ReportDTO getReportById(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        return convertToDTO(report);
    }

    private ReportDTO convertToDTO(Report report) {
        return convertToDTO(report, feedbackRepository.findByReportId(report.getId()).orElse(null));
    }

    private ReportDTO convertToDTO(Report report, Feedback feedback) {
        ReportDTO dto = new ReportDTO();
        dto.setId(report.getId());
        dto.setTitle(report.getTitle());
        dto.setDescription(report.getDescription());
        dto.setStatus(report.getStatus());
        dto.setCategoryId(report.getCategory().getId());
        dto.setCategoryName(report.getCategory().getCategoryName());
        dto.setReporterId(report.getReporter() != null ? report.getReporter().getId() : null);
        dto.setReporterUsername(report.getReporter() != null ? report.getReporter().getUsername() : "N/A");

        // Mapped to the strict village
        if (report.getIncidentVillage() != null) {
            dto.setIncidentLocationId(report.getIncidentVillage().getId());
            dto.setIncidentLocationName(report.getIncidentVillage().getName() + " Village");
        } else {
            dto.setIncidentLocationName("N/A");
        }

        dto.setCreatedAt(report.getCreatedAt());
        dto.setSlaDeadline(isFinalizedReportStatus(report.getStatus()) ? null : report.getSlaDeadline());
        applyFeedbackToDTO(report, dto, feedback);
        return dto;
    }

    private boolean isFinalizedReportStatus(String status) {
        return "RESOLVED".equalsIgnoreCase(status) || "PENDING_REPORTER_CONFIRMATION".equalsIgnoreCase(status);
    }

    private List<ReportDTO> convertReportsToDTOWithFeedback(List<Report> reports) {
        if (reports.isEmpty()) {
            return List.of();
        }

        List<Long> reportIds = reports.stream().map(Report::getId).toList();
        Map<Long, Feedback> feedbackByReportId = feedbackRepository.findByReportIdIn(reportIds).stream()
                .collect(Collectors.toMap(feedback -> feedback.getReport().getId(), Function.identity(), (a, b) -> a));

        return reports.stream()
                .map(report -> convertToDTO(report, feedbackByReportId.get(report.getId())))
                .toList();
    }

    private Page<ReportDTO> convertReportPageToDTOPage(Page<Report> reportPage) {
        List<ReportDTO> items = convertReportsToDTOWithFeedback(reportPage.getContent());
        return new PageImpl<>(items, reportPage.getPageable(), reportPage.getTotalElements());
    }

    private Page<Report> findVisibleReportsPage(User user, Pageable pageable) {
        User.UserLevelType effectiveLevelType = resolveEffectiveLevelType(user);

        return switch (effectiveLevelType) {
            case NATIONAL_ADMIN -> reportRepository.findAll(pageable);
            case PROVINCE_GOVERNOR -> user.getProvince() == null
                    ? Page.empty(pageable)
                    : reportRepository.findByCurrentEscalationLevelAndIncidentVillageCellSectorDistrictProvinceId(
                    Report.EscalationLevel.AT_PROVINCE, user.getProvince().getId(), pageable);
            case DISTRICT_MAYOR -> user.getDistrict() == null
                    ? Page.empty(pageable)
                    : reportRepository.findByCurrentEscalationLevelAndIncidentVillageCellSectorDistrictId(
                    Report.EscalationLevel.AT_DISTRICT, user.getDistrict().getId(), pageable);
            case SECTOR_LEADER -> user.getSector() == null
                    ? Page.empty(pageable)
                    : reportRepository.findByCurrentEscalationLevelAndIncidentVillageCellSectorId(
                    Report.EscalationLevel.AT_SECTOR, user.getSector().getId(), pageable);
            case CELL_LEADER -> user.getCell() == null
                    ? Page.empty(pageable)
                    : reportRepository.findByCurrentEscalationLevelAndIncidentVillageCellId(
                    Report.EscalationLevel.AT_CELL, user.getCell().getId(), pageable);
            case VILLAGE_LEADER -> user.getVillage() == null
                    ? Page.empty(pageable)
                    : reportRepository.findByCurrentEscalationLevelAndIncidentVillageId(
                    Report.EscalationLevel.AT_VILLAGE, user.getVillage().getId(), pageable);
            case CITIZEN -> reportRepository.findByReporterId(user.getId(), pageable);
        };
    }

    @Transactional
    public void escalateReport(Long reportId, String username) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));
        User actor = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        validateActorCanHandleCurrentLevel(report, actor);
        if ("PENDING_REPORTER_CONFIRMATION".equalsIgnoreCase(report.getStatus())
                || "RESOLVED".equalsIgnoreCase(report.getStatus())) {
            throw new RuntimeException("Cannot escalate a report that is already solved or pending reporter confirmation");
        }

        // Manual leader escalation is allowed at any time while the ticket is still active.
        advanceEscalation(report, actor, false);
    }

    @Transactional
    public void resolveReport(Long reportId, String username) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));
        User actor = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        validateActorCanHandleCurrentLevel(report, actor);
        if ("RESOLVED".equalsIgnoreCase(report.getStatus())) {
            throw new RuntimeException("Report is already resolved");
        }

        report.setStatus("PENDING_REPORTER_CONFIRMATION");
        report.setSlaDeadline(null);
        reportRepository.save(report);

        GovernmentLevelType levelType = toGovernmentLevelType(report.getCurrentEscalationLevel());
        slaTimerRepository.findByReportId(reportId).ifPresent(timer -> {
            timer.setStatus(SlaTimer.SlaTimerStatus.COMPLETED);
            timer.setCompletedAt(LocalDateTime.now());
            timer.setBreached(false);
            timer.setAutoFixedByScheduler(false);
            timer.setAutoFixedAt(null);
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
            report.setSlaDeadline(null);
            reportRepository.saveAndFlush(report);

            slaTimerRepository.findByReportId(report.getId()).ifPresent(timer -> {
                timer.setStatus(SlaTimer.SlaTimerStatus.COMPLETED);
                timer.setCompletedAt(LocalDateTime.now());
                timer.setBreached(false);
                timer.setAutoFixedByScheduler(false);
                timer.setAutoFixedAt(null);
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
                LocalDateTime.now().plusMinutes(1440)
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
            if (report == null) {
                continue;
            }

            if ("RESOLVED".equalsIgnoreCase(report.getStatus())
                    || "PENDING_REPORTER_CONFIRMATION".equalsIgnoreCase(report.getStatus())) {
                // Defensive cleanup: keep resolved/confirmed reports from being reconsidered.
                finalizeTimerAsCompleted(timer);
                continue;
            }

            boolean changed = false;
            if (isReportOverdue(report) && report.getCurrentEscalationLevel() != Report.EscalationLevel.AT_NATIONAL) {
                // Move only one level per run so each leadership tier gets its SLA handling window.
                advanceEscalation(report, null, true);
                changed = true;
            } else if (isReportOverdue(report) && report.getCurrentEscalationLevel() == Report.EscalationLevel.AT_NATIONAL) {
                markBreached(report, null);
                changed = true;
            }

            if (changed) {
                processed++;
            }
        }

        return processed;
    }

    private void finalizeTimerAsCompleted(SlaTimer timer) {
        timer.setStatus(SlaTimer.SlaTimerStatus.COMPLETED);
        if (timer.getCompletedAt() == null) {
            timer.setCompletedAt(LocalDateTime.now());
        }
        timer.setBreached(false);
        timer.setAutoFixedByScheduler(true);
        timer.setAutoFixedAt(LocalDateTime.now());
        slaTimerRepository.save(timer);
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
                .map(config -> {
                    Integer minutes = config.getDurationMinutes();
                    return LocalDateTime.now().plusMinutes(minutes != null ? minutes : 60);
                })
                .orElseGet(() -> fallbackDeadline != null ? fallbackDeadline : LocalDateTime.now().plusMinutes(60));
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
        timer.setAutoFixedByScheduler(false);
        timer.setAutoFixedAt(null);
        slaTimerRepository.save(timer);
    }

    private void writeHistory(Report report, User actedBy, GovernmentLevelType fromLevel, GovernmentLevelType toLevel,
                              String action, String notes) {
        ReportHistory history = new ReportHistory();
        history.setReport(report);
        User actor = actedBy != null ? actedBy : report.getReporter();
        if (actor == null) {
            return;
        }
        history.setActedBy(actor);
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
        LocalDateTime deadline = resolveSlaDeadline(report.getCategory().getId(), toLevel, LocalDateTime.now().plusMinutes(1440));
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

    private void applyFeedbackToDTO(Report report, ReportDTO dto, Feedback feedback) {
        if (feedback != null) {
            dto.setReporterApproved(feedback.getApproved());
            dto.setServiceRating(feedback.getRating());
            dto.setServiceComment(feedback.getComment());
            dto.setReporterConfirmedAt(feedback.getConfirmedAt());
        } else {
            dto.setReporterApproved(null);
            dto.setServiceRating(null);
            dto.setServiceComment(null);
            dto.setReporterConfirmedAt(null);
        }

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
        User.UserLevelType effectiveLevelType = resolveEffectiveLevelType(user);

        return switch (effectiveLevelType) {
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

    private User.UserLevelType resolveEffectiveLevelType(User user) {
        if (user.getLevelType() != null && user.getLevelType() != User.UserLevelType.CITIZEN) {
            return user.getLevelType();
        }

        String roleName = user.getRole() != null ? user.getRole().getRoleName() : null;
        if (roleName != null) {
            String normalizedRole = roleName.trim().toUpperCase(Locale.ROOT);
            if ("ADMIN".equals(normalizedRole) || "ROLE_ADMIN".equals(normalizedRole) || normalizedRole.endsWith("ADMIN")) {
                return User.UserLevelType.NATIONAL_ADMIN;
            }
            if ("LEADER".equals(normalizedRole) || "ROLE_LEADER".equals(normalizedRole) || normalizedRole.endsWith("LEADER")) {
                if (user.getVillage() != null) return User.UserLevelType.VILLAGE_LEADER;
                if (user.getCell() != null) return User.UserLevelType.CELL_LEADER;
                if (user.getSector() != null) return User.UserLevelType.SECTOR_LEADER;
                if (user.getDistrict() != null) return User.UserLevelType.DISTRICT_MAYOR;
                if (user.getProvince() != null) return User.UserLevelType.PROVINCE_GOVERNOR;
            }
        }

        return user.getLevelType() != null ? user.getLevelType() : User.UserLevelType.CITIZEN;
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

    private void validateActorCanHandleCurrentLevel(Report report, User actor) {
        User.UserLevelType levelType = resolveEffectiveLevelType(actor);
        boolean allowed = switch (levelType) {
            case NATIONAL_ADMIN -> true;
            case PROVINCE_GOVERNOR -> report.getCurrentEscalationLevel() == Report.EscalationLevel.AT_PROVINCE
                    && matchesProvinceScope(report, actor);
            case DISTRICT_MAYOR -> report.getCurrentEscalationLevel() == Report.EscalationLevel.AT_DISTRICT
                    && matchesDistrictScope(report, actor);
            case SECTOR_LEADER -> report.getCurrentEscalationLevel() == Report.EscalationLevel.AT_SECTOR
                    && matchesSectorScope(report, actor);
            case CELL_LEADER -> report.getCurrentEscalationLevel() == Report.EscalationLevel.AT_CELL
                    && matchesCellScope(report, actor);
            case VILLAGE_LEADER -> report.getCurrentEscalationLevel() == Report.EscalationLevel.AT_VILLAGE
                    && matchesVillageScope(report, actor);
            case CITIZEN -> false;
        };

        if (!allowed) {
            throw new RuntimeException("You are not allowed to handle this report at its current escalation level");
        }
    }
}