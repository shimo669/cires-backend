package com.cires.ciresbackend.controller;

import com.cires.ciresbackend.dto.ApiResponse;
import com.cires.ciresbackend.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leader")
@RequiredArgsConstructor
@CrossOrigin("*")
public class LeaderController {

    private final ReportService reportService;

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<?>> getVisibleReports() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(new ApiResponse<>(200, "Reports retrieved successfully", reportService.getVisibleReportsForUser(username)));
    }
}

