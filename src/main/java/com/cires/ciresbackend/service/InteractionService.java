package com.cires.ciresbackend.service;

import com.cires.ciresbackend.dto.FeedbackRequestDTO;
import com.cires.ciresbackend.dto.HistoryResponseDTO;
import com.cires.ciresbackend.entity.*;
import com.cires.ciresbackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InteractionService {

    private final FeedbackRepository feedbackRepository;
    private final ReportRepository reportRepository;
    private final ReportHistoryRepository historyRepository;
    private final UserRepository userRepository;

    public void submitFeedback(Long reportId, FeedbackRequestDTO request, String username) {
        Report report = reportRepository.findById(reportId).orElseThrow();
        User user = userRepository.findByUsername(username).orElseThrow();

        Feedback feedback = new Feedback();
        feedback.setReport(report);
        feedback.setCitizen(user);
        feedback.setRating(request.getRating());
        feedback.setComment(request.getComment());
        feedbackRepository.save(feedback);
    }

    public List<HistoryResponseDTO> getReportHistory(Long reportId) {
        return historyRepository.findByReportId(reportId).stream().map(history -> {
            HistoryResponseDTO dto = new HistoryResponseDTO();
            dto.setAction(history.getAction());
            dto.setNotes(history.getNotes());
            dto.setTimestamp(history.getActionTimestamp());
            dto.setActedBy(history.getActedBy().getUsername());
            return dto;
        }).collect(Collectors.toList());
    }
}