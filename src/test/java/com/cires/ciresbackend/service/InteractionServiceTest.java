package com.cires.ciresbackend.service;

import com.cires.ciresbackend.dto.FeedbackRequestDTO;
import com.cires.ciresbackend.dto.HistoryResponseDTO;
import com.cires.ciresbackend.entity.Feedback;
import com.cires.ciresbackend.entity.Report;
import com.cires.ciresbackend.entity.ReportHistory;
import com.cires.ciresbackend.entity.User;
import com.cires.ciresbackend.exception.ForbiddenActionException;
import com.cires.ciresbackend.exception.InvalidRequestException;
import com.cires.ciresbackend.repository.FeedbackRepository;
import com.cires.ciresbackend.repository.ReportHistoryRepository;
import com.cires.ciresbackend.repository.ReportRepository;
import com.cires.ciresbackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InteractionServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;
    @Mock
    private ReportRepository reportRepository;
    @Mock
    private ReportHistoryRepository historyRepository;
    @Mock
    private UserRepository userRepository;

    private InteractionService interactionService;

    @BeforeEach
    void setUp() {
        interactionService = new InteractionService(feedbackRepository, reportRepository, historyRepository, userRepository);
    }

    @Test
    void submitFeedback_success_savesFeedback() {
        User reporter = new User();
        reporter.setId(1L);
        reporter.setUsername("alice");

        Report report = new Report();
        report.setId(7L);
        report.setReporter(reporter);
        report.setStatus("RESOLVED");

        FeedbackRequestDTO request = new FeedbackRequestDTO(5, "Great service");

        when(reportRepository.findById(7L)).thenReturn(Optional.of(report));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(reporter));
        when(feedbackRepository.findByReportId(7L)).thenReturn(Optional.empty());

        interactionService.submitFeedback(7L, request, "alice");

        ArgumentCaptor<Feedback> captor = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackRepository).save(captor.capture());
        assertEquals(5, captor.getValue().getRating());
        assertEquals(true, captor.getValue().getApproved());
    }

    @Test
    void submitFeedback_nonReporter_forbidden() {
        User reporter = new User();
        reporter.setId(1L);

        User anotherUser = new User();
        anotherUser.setId(2L);
        anotherUser.setUsername("other");

        Report report = new Report();
        report.setId(8L);
        report.setReporter(reporter);
        report.setStatus("RESOLVED");

        when(reportRepository.findById(8L)).thenReturn(Optional.of(report));
        when(userRepository.findByUsername("other")).thenReturn(Optional.of(anotherUser));

        assertThrows(ForbiddenActionException.class,
                () -> interactionService.submitFeedback(8L, new FeedbackRequestDTO(5, "ok"), "other"));
    }

    @Test
    void submitFeedback_invalidRating_throws() {
        User reporter = new User();
        reporter.setId(1L);
        reporter.setUsername("alice");

        Report report = new Report();
        report.setId(9L);
        report.setReporter(reporter);
        report.setStatus("RESOLVED");

        when(reportRepository.findById(9L)).thenReturn(Optional.of(report));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(reporter));

        assertThrows(InvalidRequestException.class,
                () -> interactionService.submitFeedback(9L, new FeedbackRequestDTO(6, "too high"), "alice"));
    }

    @Test
    void getReportHistory_handlesSystemActor() {
        ReportHistory history = new ReportHistory();
        history.setAction("AUTO_ESCALATED");
        history.setNotes("System escalated due to overdue SLA");
        history.setActedBy(null);
        history.setActionTimestamp(java.time.LocalDateTime.now());

        when(historyRepository.findByReportIdOrderByActionTimestampDesc(3L)).thenReturn(List.of(history));

        List<HistoryResponseDTO> result = interactionService.getReportHistory(3L);

        assertEquals(1, result.size());
        assertEquals("SYSTEM", result.get(0).getActedBy());
    }
}

