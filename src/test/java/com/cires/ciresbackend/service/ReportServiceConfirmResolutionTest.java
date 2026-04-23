package com.cires.ciresbackend.service;

import com.cires.ciresbackend.dto.ReportConfirmationRequestDTO;
import com.cires.ciresbackend.entity.Category;
import com.cires.ciresbackend.entity.Cell;
import com.cires.ciresbackend.entity.District;
import com.cires.ciresbackend.entity.Feedback;
import com.cires.ciresbackend.entity.Province;
import com.cires.ciresbackend.entity.Report;
import com.cires.ciresbackend.entity.ReportHistory;
import com.cires.ciresbackend.entity.Role;
import com.cires.ciresbackend.entity.Sector;
import com.cires.ciresbackend.entity.SlaTimer;
import com.cires.ciresbackend.entity.User;
import com.cires.ciresbackend.entity.Village;
import com.cires.ciresbackend.repository.CategoryRepository;
import com.cires.ciresbackend.repository.FeedbackRepository;
import com.cires.ciresbackend.repository.ReportHistoryRepository;
import com.cires.ciresbackend.repository.ReportRepository;
import com.cires.ciresbackend.repository.SlaConfigRepository;
import com.cires.ciresbackend.repository.SlaTimerRepository;
import com.cires.ciresbackend.repository.UserRepository;
import com.cires.ciresbackend.repository.VillageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceConfirmResolutionTest {

    @Mock
    private ReportRepository reportRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private VillageRepository villageRepository;
    @Mock
    private SlaConfigRepository slaConfigRepository;
    @Mock
    private SlaTimerRepository slaTimerRepository;
    @Mock
    private ReportHistoryRepository reportHistoryRepository;
    @Mock
    private FeedbackRepository feedbackRepository;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(
                reportRepository,
                userRepository,
                categoryRepository,
                villageRepository,
                slaConfigRepository,
                slaTimerRepository,
                reportHistoryRepository,
                feedbackRepository
        );
    }

    @Test
    void confirmResolutionApprove_setsResolvedAndStoresRating() {
        User reporter = new User();
        reporter.setId(10L);
        reporter.setUsername("alice");

        Category category = new Category();
        category.setId(22L);

        Report report = new Report();
        report.setId(1L);
        report.setReporter(reporter);
        report.setCategory(category);
        report.setStatus("PENDING_REPORTER_CONFIRMATION");
        report.setCurrentEscalationLevel(Report.EscalationLevel.AT_CELL);
        report.setSlaDeadline(java.time.LocalDateTime.now().plusHours(2));

        SlaTimer timer = new SlaTimer();
        timer.setDeadline(java.time.LocalDateTime.now().plusHours(1));

        ReportConfirmationRequestDTO request = new ReportConfirmationRequestDTO(true, 4, "Well handled");

        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(reporter));
        when(slaTimerRepository.findByReportId(1L)).thenReturn(Optional.of(timer));
        when(feedbackRepository.findByReportId(1L)).thenReturn(Optional.empty());

        reportService.confirmResolution(1L, request, "alice");

        assertEquals("RESOLVED", report.getStatus());
        assertEquals(null, report.getSlaDeadline());

        ArgumentCaptor<Feedback> feedbackCaptor = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackRepository).save(feedbackCaptor.capture());
        assertEquals(true, feedbackCaptor.getValue().getApproved());
        assertEquals(4, feedbackCaptor.getValue().getRating());

        ArgumentCaptor<ReportHistory> historyCaptor = ArgumentCaptor.forClass(ReportHistory.class);
        verify(reportHistoryRepository).save(historyCaptor.capture());
        assertEquals("REPORTER_CONFIRMED", historyCaptor.getValue().getAction());

        verify(slaTimerRepository).save(any(SlaTimer.class));
    }

    @Test
    void confirmResolutionReject_reopensAndStartsSlaTimer() {
        User reporter = new User();
        reporter.setId(11L);
        reporter.setUsername("bob");

        Category category = new Category();
        category.setId(100L);

        Report report = new Report();
        report.setId(2L);
        report.setReporter(reporter);
        report.setCategory(category);
        report.setStatus("PENDING_REPORTER_CONFIRMATION");
        report.setCurrentEscalationLevel(Report.EscalationLevel.AT_VILLAGE);
        report.setSlaDeadline(java.time.LocalDateTime.now().plusHours(2));

        when(reportRepository.findById(2L)).thenReturn(Optional.of(report));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(reporter));
        when(slaConfigRepository.findByCategoryIdAndLevelType(any(), any())).thenReturn(Optional.empty());
        when(slaTimerRepository.findByReportId(2L)).thenReturn(Optional.empty());
        when(feedbackRepository.findByReportId(2L)).thenReturn(Optional.empty());

        reportService.confirmResolution(2L, new ReportConfirmationRequestDTO(false, null, "Not solved"), "bob");

        assertEquals("REOPENED", report.getStatus());
        assertTrue(report.getSlaDeadline() != null);
        verify(slaTimerRepository).save(any(SlaTimer.class));
    }

    @Test
    void confirmResolutionApprove_requiresRating() {
        User reporter = new User();
        reporter.setId(99L);
        reporter.setUsername("carol");

        Category category = new Category();
        category.setId(9L);

        Report report = new Report();
        report.setId(3L);
        report.setReporter(reporter);
        report.setCategory(category);
        report.setStatus("PENDING_REPORTER_CONFIRMATION");
        report.setCurrentEscalationLevel(Report.EscalationLevel.AT_SECTOR);

        when(reportRepository.findById(3L)).thenReturn(Optional.of(report));
        when(userRepository.findByUsername("carol")).thenReturn(Optional.of(reporter));

        assertThrows(RuntimeException.class,
                () -> reportService.confirmResolution(3L, new ReportConfirmationRequestDTO(true, null, "ok"), "carol"));
    }

    @Test
    void autoEscalateOverdueReports_usesReporterWhenActorIsAutomatic() {
        User reporter = new User();
        reporter.setId(30L);
        reporter.setUsername("scheduler-fallback-reporter");

        Category category = new Category();
        category.setId(40L);

        Report report = new Report();
        report.setId(5L);
        report.setReporter(reporter);
        report.setCategory(category);
        report.setStatus("PENDING");
        report.setCurrentEscalationLevel(Report.EscalationLevel.AT_VILLAGE);
        report.setSlaDeadline(java.time.LocalDateTime.now().minusMinutes(2));

        SlaTimer timer = new SlaTimer();
        timer.setReport(report);
        timer.setStatus(SlaTimer.SlaTimerStatus.ACTIVE);
        timer.setDeadline(java.time.LocalDateTime.now().minusMinutes(1));

        when(slaTimerRepository.findByStatusAndDeadlineBefore(any(), any())).thenReturn(java.util.List.of(timer));
        when(slaConfigRepository.findByCategoryIdAndLevelType(any(), any())).thenReturn(Optional.empty());
        when(slaTimerRepository.findByReportId(5L)).thenReturn(Optional.of(timer));

        int processed = reportService.autoEscalateOverdueReports();

        assertEquals(1, processed);
        assertEquals(Report.EscalationLevel.AT_CELL, report.getCurrentEscalationLevel());

        ArgumentCaptor<ReportHistory> historyCaptor = ArgumentCaptor.forClass(ReportHistory.class);
        verify(reportHistoryRepository, atLeastOnce()).save(historyCaptor.capture());
        assertTrue(historyCaptor.getAllValues().stream().allMatch(h -> h.getActedBy() != null));
    }

    @Test
    void getVisibleReportsForUser_cellLeaderWithCellAssignmentSeesCellEscalatedReports() {
        Province province = new Province();
        province.setId(1L);
        province.setName("Kigali");

        District district = new District();
        district.setId(2L);
        district.setName("Gasabo");
        district.setProvince(province);

        Sector sector = new Sector();
        sector.setId(3L);
        sector.setName("Kimironko");
        sector.setDistrict(district);

        Cell cell = new Cell();
        cell.setId(4L);
        cell.setName("Bibare");
        cell.setSector(sector);

        Village village = new Village();
        village.setId(5L);
        village.setName("Village A");
        village.setCell(cell);

        User leader = new User();
        leader.setId(70L);
        leader.setUsername("cell-leader");
        leader.setCell(cell);

        Role leaderRole = new Role();
        leaderRole.setRoleName("LEADER");
        leader.setRole(leaderRole);
        leader.setLevelType(User.UserLevelType.CITIZEN);

        User reporter = new User();
        reporter.setId(71L);
        reporter.setUsername("citizen-a");

        Category category = new Category();
        category.setId(10L);
        category.setCategoryName("Road");

        Report visibleReport = new Report();
        visibleReport.setId(100L);
        visibleReport.setTitle("Visible report");
        visibleReport.setDescription("Cell-level escalated report");
        visibleReport.setReporter(reporter);
        visibleReport.setCategory(category);
        visibleReport.setIncidentVillage(village);
        visibleReport.setCurrentEscalationLevel(Report.EscalationLevel.AT_CELL);
        visibleReport.setStatus("ESCALATED");

        Report hiddenReport = new Report();
        hiddenReport.setId(101L);
        hiddenReport.setTitle("Hidden report");
        hiddenReport.setDescription("Village-level report");
        hiddenReport.setReporter(reporter);
        hiddenReport.setCategory(category);
        hiddenReport.setIncidentVillage(village);
        hiddenReport.setCurrentEscalationLevel(Report.EscalationLevel.AT_VILLAGE);
        hiddenReport.setStatus("PENDING");

        when(userRepository.findByUsername("cell-leader")).thenReturn(Optional.of(leader));
        when(reportRepository.findByCurrentEscalationLevelAndIncidentVillageCellId(any(), any(), any()))
                .thenReturn(new PageImpl<>(java.util.List.of(visibleReport)));
        when(feedbackRepository.findByReportIdIn(any())).thenReturn(java.util.List.of());

        java.util.List<com.cires.ciresbackend.dto.ReportDTO> reports = reportService.getVisibleReportsForUser("cell-leader");

        assertEquals(1, reports.size());
        assertEquals(100L, reports.get(0).getId());
        assertFalse(reports.stream().anyMatch(r -> r.getId().equals(101L)));
    }

    @Test
    void autoEscalateOverdueReports_resolvedReportIsNotEscalatedAndTimerIsCompleted() {
        User reporter = new User();
        reporter.setId(81L);
        reporter.setUsername("resolved-reporter");

        Category category = new Category();
        category.setId(55L);

        Report report = new Report();
        report.setId(200L);
        report.setReporter(reporter);
        report.setCategory(category);
        report.setStatus("RESOLVED");
        report.setCurrentEscalationLevel(Report.EscalationLevel.AT_CELL);
        report.setSlaDeadline(java.time.LocalDateTime.now().minusMinutes(10));

        SlaTimer timer = new SlaTimer();
        timer.setReport(report);
        timer.setStatus(SlaTimer.SlaTimerStatus.ACTIVE);
        timer.setDeadline(java.time.LocalDateTime.now().minusMinutes(5));

        when(slaTimerRepository.findByStatusAndDeadlineBefore(any(), any())).thenReturn(java.util.List.of(timer));

        int processed = reportService.autoEscalateOverdueReports();

        assertEquals(0, processed);
        assertEquals(Report.EscalationLevel.AT_CELL, report.getCurrentEscalationLevel());

        ArgumentCaptor<SlaTimer> timerCaptor = ArgumentCaptor.forClass(SlaTimer.class);
        verify(slaTimerRepository).save(timerCaptor.capture());
        assertEquals(SlaTimer.SlaTimerStatus.COMPLETED, timerCaptor.getValue().getStatus());
        assertFalse(Boolean.TRUE.equals(timerCaptor.getValue().getBreached()));
        verify(reportHistoryRepository, never()).save(any());
    }

    @Test
    void getVisibleReportsPageForUser_returnsPagedDataForLeaderScope() {
        Province province = new Province();
        province.setId(1L);
        province.setName("Kigali");

        District district = new District();
        district.setId(2L);
        district.setName("Gasabo");
        district.setProvince(province);

        Sector sector = new Sector();
        sector.setId(3L);
        sector.setName("Kimironko");
        sector.setDistrict(district);

        Cell cell = new Cell();
        cell.setId(4L);
        cell.setName("Bibare");
        cell.setSector(sector);

        Village village = new Village();
        village.setId(5L);
        village.setName("Village A");
        village.setCell(cell);

        User leader = new User();
        leader.setId(70L);
        leader.setUsername("cell-leader");
        leader.setCell(cell);

        Role leaderRole = new Role();
        leaderRole.setRoleName("LEADER");
        leader.setRole(leaderRole);
        leader.setLevelType(User.UserLevelType.CITIZEN);

        User reporter = new User();
        reporter.setId(71L);
        reporter.setUsername("citizen-a");

        Category category = new Category();
        category.setId(10L);
        category.setCategoryName("Road");

        Report visibleReport = new Report();
        visibleReport.setId(300L);
        visibleReport.setTitle("Visible report");
        visibleReport.setDescription("Cell-level escalated report");
        visibleReport.setReporter(reporter);
        visibleReport.setCategory(category);
        visibleReport.setIncidentVillage(village);
        visibleReport.setCurrentEscalationLevel(Report.EscalationLevel.AT_CELL);
        visibleReport.setStatus("ESCALATED");

        when(userRepository.findByUsername("cell-leader")).thenReturn(Optional.of(leader));
        when(reportRepository.findByCurrentEscalationLevelAndIncidentVillageCellId(any(), any(), any()))
                .thenReturn(new PageImpl<>(java.util.List.of(visibleReport), PageRequest.of(0, 1), 1));
        when(feedbackRepository.findByReportIdIn(any())).thenReturn(java.util.List.of());

        org.springframework.data.domain.Page<com.cires.ciresbackend.dto.ReportDTO> page =
                reportService.getVisibleReportsPageForUser("cell-leader", 0, 1);

        assertEquals(1, page.getContent().size());
        assertEquals(1, page.getTotalElements());
        assertEquals(300L, page.getContent().get(0).getId());
    }

    @Test
    void escalateReport_rejectsLeaderWhenReportIsAtDifferentLevel() {
        Province province = new Province();
        province.setId(1L);
        province.setName("Kigali");

        District district = new District();
        district.setId(2L);
        district.setName("Gasabo");
        district.setProvince(province);

        Sector sector = new Sector();
        sector.setId(3L);
        sector.setName("Kimironko");
        sector.setDistrict(district);

        Cell cell = new Cell();
        cell.setId(4L);
        cell.setName("Bibare");
        cell.setSector(sector);

        Village village = new Village();
        village.setId(5L);
        village.setName("Village A");
        village.setCell(cell);

        User villageLeader = new User();
        villageLeader.setId(500L);
        villageLeader.setUsername("village-leader");
        villageLeader.setVillage(village);
        Role leaderRole = new Role();
        leaderRole.setRoleName("LEADER");
        villageLeader.setRole(leaderRole);

        User reporter = new User();
        reporter.setId(71L);
        reporter.setUsername("citizen-a");

        Category category = new Category();
        category.setId(10L);
        category.setCategoryName("Road");

        Report report = new Report();
        report.setId(350L);
        report.setTitle("Escalated report");
        report.setDescription("Already at cell");
        report.setReporter(reporter);
        report.setCategory(category);
        report.setIncidentVillage(village);
        report.setCurrentEscalationLevel(Report.EscalationLevel.AT_CELL);
        report.setStatus("ESCALATED");

        when(reportRepository.findById(350L)).thenReturn(Optional.of(report));
        when(userRepository.findByUsername("village-leader")).thenReturn(Optional.of(villageLeader));

        assertThrows(RuntimeException.class, () -> reportService.escalateReport(350L, "village-leader"));
    }

    @Test
    void escalateReport_allowsLeaderEscalationBeforeDeadline() {
        Province province = new Province();
        province.setId(1L);
        province.setName("Kigali");

        District district = new District();
        district.setId(2L);
        district.setName("Gasabo");
        district.setProvince(province);

        Sector sector = new Sector();
        sector.setId(3L);
        sector.setName("Kimironko");
        sector.setDistrict(district);

        Cell cell = new Cell();
        cell.setId(4L);
        cell.setName("Bibare");
        cell.setSector(sector);

        Village village = new Village();
        village.setId(5L);
        village.setName("Village A");
        village.setCell(cell);

        User villageLeader = new User();
        villageLeader.setId(501L);
        villageLeader.setUsername("village-leader");
        villageLeader.setVillage(village);
        Role leaderRole = new Role();
        leaderRole.setRoleName("LEADER");
        villageLeader.setRole(leaderRole);

        User reporter = new User();
        reporter.setId(72L);
        reporter.setUsername("citizen-b");

        Category category = new Category();
        category.setId(11L);
        category.setCategoryName("Road");

        Report report = new Report();
        report.setId(360L);
        report.setTitle("Need early escalation");
        report.setDescription("Escalate before SLA deadline");
        report.setReporter(reporter);
        report.setCategory(category);
        report.setIncidentVillage(village);
        report.setCurrentEscalationLevel(Report.EscalationLevel.AT_VILLAGE);
        report.setStatus("PENDING");
        report.setSlaDeadline(java.time.LocalDateTime.now().plusHours(12));

        when(reportRepository.findById(360L)).thenReturn(Optional.of(report));
        when(userRepository.findByUsername("village-leader")).thenReturn(Optional.of(villageLeader));
        when(slaConfigRepository.findByCategoryIdAndLevelType(any(), any())).thenReturn(Optional.empty());
        when(slaTimerRepository.findByReportId(360L)).thenReturn(Optional.empty());

        reportService.escalateReport(360L, "village-leader");

        assertEquals(Report.EscalationLevel.AT_CELL, report.getCurrentEscalationLevel());
        assertEquals("ESCALATED", report.getStatus());
    }
}

