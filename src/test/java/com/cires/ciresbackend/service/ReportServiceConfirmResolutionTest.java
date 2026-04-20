package com.cires.ciresbackend.service;

import com.cires.ciresbackend.dto.ReportConfirmationRequestDTO;
import com.cires.ciresbackend.entity.Category;
import com.cires.ciresbackend.entity.Feedback;
import com.cires.ciresbackend.entity.Report;
import com.cires.ciresbackend.entity.ReportHistory;
import com.cires.ciresbackend.entity.SlaTimer;
import com.cires.ciresbackend.entity.User;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

        SlaTimer timer = new SlaTimer();
        timer.setDeadline(java.time.LocalDateTime.now().plusHours(1));

        ReportConfirmationRequestDTO request = new ReportConfirmationRequestDTO(true, 4, "Well handled");

        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(reporter));
        when(slaTimerRepository.findByReportId(1L)).thenReturn(Optional.of(timer));
        when(feedbackRepository.findByReportId(1L)).thenReturn(Optional.empty());

        reportService.confirmResolution(1L, request, "alice");

        assertEquals("RESOLVED", report.getStatus());

        ArgumentCaptor<Feedback> feedbackCaptor = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackRepository).save(feedbackCaptor.capture());
        assertEquals(true, feedbackCaptor.getValue().getApproved());
        assertEquals(4, feedbackCaptor.getValue().getRating());

        ArgumentCaptor<ReportHistory> historyCaptor = ArgumentCaptor.forClass(ReportHistory.class);
        verify(reportHistoryRepository).save(historyCaptor.capture());
        assertEquals("REPORTER_CONFIRMED", historyCaptor.getValue().getAction());
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

        when(reportRepository.findById(2L)).thenReturn(Optional.of(report));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(reporter));
        when(slaConfigRepository.findByCategoryIdAndLevelType(any(), any())).thenReturn(Optional.empty());
        when(slaTimerRepository.findByReportId(2L)).thenReturn(Optional.empty());
        when(feedbackRepository.findByReportId(2L)).thenReturn(Optional.empty());

        reportService.confirmResolution(2L, new ReportConfirmationRequestDTO(false, null, "Not solved"), "bob");

        assertEquals("REOPENED", report.getStatus());
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
}

