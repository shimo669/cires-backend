package com.cires.ciresbackend.service;

import com.cires.ciresbackend.dto.CreateReportRequest;
import com.cires.ciresbackend.dto.ReportDTO;
import com.cires.ciresbackend.entity.*;
import com.cires.ciresbackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final VillageRepository villageRepository;
    private final SlaConfigRepository slaConfigRepository;

    @Transactional
    public ReportDTO createReport(CreateReportRequest request, String username) {
        // Get the reporter
        User reporter = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        // Get the category
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        // In the new architecture, the incident location MUST be a Village!
        Village incidentVillage = villageRepository.findById(request.getIncidentLocationId())
                .orElseThrow(() -> new IllegalArgumentException("Incident village not found"));

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
        report.setSlaDeadline(request.getSlaDeadline());

        Report savedReport = reportRepository.save(report);

        return convertToDTO(savedReport);
    }

    public List<ReportDTO> getMyReports(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        List<Report> reports = reportRepository.findByReporterId(user.getId());
        return reports.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<ReportDTO> getReportsByLevel(String level) {
        // Now filters by the new EscalationLevel Enum
        Report.EscalationLevel escalationLevel = Report.EscalationLevel.valueOf(level.toUpperCase());

        List<Report> reports = reportRepository.findAll().stream()
                .filter(r -> r.getCurrentEscalationLevel() == escalationLevel)
                .collect(Collectors.toList());

        return reports.stream().map(this::convertToDTO).collect(Collectors.toList());
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
        return dto;
    }

    @Transactional
    public void escalateReport(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        // STRICT ESCALATION ENGINE LOGIC
        switch (report.getCurrentEscalationLevel()) {
            case AT_VILLAGE:
                report.setCurrentEscalationLevel(Report.EscalationLevel.AT_CELL);
                break;
            case AT_CELL:
                report.setCurrentEscalationLevel(Report.EscalationLevel.AT_SECTOR);
                break;
            case AT_SECTOR:
                report.setCurrentEscalationLevel(Report.EscalationLevel.AT_DISTRICT);
                break;
            case AT_DISTRICT:
                report.setCurrentEscalationLevel(Report.EscalationLevel.AT_PROVINCE);
                break;
            case AT_PROVINCE:
                report.setCurrentEscalationLevel(Report.EscalationLevel.AT_NATIONAL);
                break;
            case AT_NATIONAL:
                throw new RuntimeException("Report is already at the highest level (NATIONAL)");
        }

        reportRepository.save(report);
    }
}