package com.cires.ciresbackend.controller;

import com.cires.ciresbackend.dto.FeedbackRequestDTO;
import com.cires.ciresbackend.service.InteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/interactions")
@RequiredArgsConstructor
@CrossOrigin("*")
public class InteractionController {

    private final InteractionService interactionService;

    @PostMapping("/reports/{reportId}/feedback")
    public ResponseEntity<?> submitFeedback(@PathVariable Long reportId,
                                            @RequestBody FeedbackRequestDTO request,
                                            Authentication authentication) {
        // authentication.getName() provides the username from the JWT
        interactionService.submitFeedback(reportId, request, authentication.getName());
        return ResponseEntity.ok("Feedback submitted successfully");
    }

    @GetMapping("/reports/{reportId}/history")
    public ResponseEntity<?> getHistory(@PathVariable Long reportId) {
        return ResponseEntity.ok(interactionService.getReportHistory(reportId));
    }
}