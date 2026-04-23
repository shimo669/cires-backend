package com.cires.ciresbackend.service;

import com.cires.ciresbackend.dto.AutoFixedSlaTimerDTO;
import com.cires.ciresbackend.entity.Category;
import com.cires.ciresbackend.entity.GovernmentLevelType;
import com.cires.ciresbackend.entity.Report;
import com.cires.ciresbackend.entity.SlaTimer;
import com.cires.ciresbackend.repository.CategoryRepository;
import com.cires.ciresbackend.repository.CellRepository;
import com.cires.ciresbackend.repository.DistrictRepository;
import com.cires.ciresbackend.repository.FeedbackRepository;
import com.cires.ciresbackend.repository.ProvinceRepository;
import com.cires.ciresbackend.repository.ReportRepository;
import com.cires.ciresbackend.repository.RoleRepository;
import com.cires.ciresbackend.repository.SectorRepository;
import com.cires.ciresbackend.repository.SlaConfigRepository;
import com.cires.ciresbackend.repository.SlaTimerRepository;
import com.cires.ciresbackend.repository.UserRepository;
import com.cires.ciresbackend.repository.VillageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceAutoFixedTimersTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private SlaConfigRepository slaConfigRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ProvinceRepository provinceRepository;
    @Mock
    private DistrictRepository districtRepository;
    @Mock
    private SectorRepository sectorRepository;
    @Mock
    private CellRepository cellRepository;
    @Mock
    private VillageRepository villageRepository;
    @Mock
    private ReportRepository reportRepository;
    @Mock
    private FeedbackRepository feedbackRepository;
    @Mock
    private SlaTimerRepository slaTimerRepository;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(
                userRepository,
                roleRepository,
                slaConfigRepository,
                categoryRepository,
                provinceRepository,
                districtRepository,
                sectorRepository,
                cellRepository,
                villageRepository,
                reportRepository,
                feedbackRepository,
                slaTimerRepository
        );
    }

    @Test
    void getAutoFixedSlaTimers_returnsMappedAuditRows() {
        Category category = new Category();
        category.setId(10L);
        category.setCategoryName("Water");

        Report report = new Report();
        report.setId(90L);
        report.setStatus("RESOLVED");
        report.setCurrentEscalationLevel(Report.EscalationLevel.AT_CELL);
        report.setCategory(category);

        SlaTimer timer = new SlaTimer();
        timer.setId(15L);
        timer.setReport(report);
        timer.setLevelType(GovernmentLevelType.CELL);
        timer.setDeadline(LocalDateTime.now().minusHours(1));
        timer.setCompletedAt(LocalDateTime.now());
        timer.setAutoFixedByScheduler(true);
        timer.setAutoFixedAt(LocalDateTime.now());

        when(slaTimerRepository.findByAutoFixedBySchedulerTrueOrderByAutoFixedAtDesc(any()))
                .thenReturn(List.of(timer));

        List<AutoFixedSlaTimerDTO> result = adminService.getAutoFixedSlaTimers(50);

        assertEquals(1, result.size());
        assertEquals(15L, result.get(0).getTimerId());
        assertEquals(90L, result.get(0).getReportId());
        assertEquals("RESOLVED", result.get(0).getReportStatus());
        assertEquals("AT_CELL", result.get(0).getEscalationLevel());
        assertEquals("CELL", result.get(0).getLevelType());
    }
}

