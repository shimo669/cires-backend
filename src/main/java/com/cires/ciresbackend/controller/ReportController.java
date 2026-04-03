package com.cires.ciresbackend.controller;

import com.cires.ciresbackend.dto.ApiResponse;
import com.cires.ciresbackend.dto.CreateReportRequest;
import com.cires.ciresbackend.dto.ReportDTO;
import com.cires.ciresbackend.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin("*")
public class ReportController {

    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

    @Autowired
    private ReportService reportService;

    @PostMapping
    public ResponseEntity<ApiResponse<?>> createReport(@RequestBody CreateReportRequest request) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            logger.info("Creating report for user: {}", username);

            ReportDTO reportDTO = reportService.createReport(request, username);
            logger.info("Report created successfully with ID: {}", reportDTO.getId());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(201, "Report created successfully", reportDTO));
        } catch (IllegalArgumentException e) {
            logger.warn("Report creation failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(400, e.getMessage()));
        } catch (Exception e) {
            logger.error("Report creation error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(500, "Error creating report"));
        }
    }

    @GetMapping("/my-reports")
    public ResponseEntity<ApiResponse<?>> getMyReports() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            logger.info("Fetching reports for user: {}", username);

            List<ReportDTO> reports = reportService.getMyReports(username);
            logger.info("Found {} reports for user: {}", reports.size(), username);

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(200, "Reports retrieved successfully", reports));
        } catch (IllegalArgumentException e) {
            logger.warn("Error fetching reports: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(400, e.getMessage()));
        } catch (Exception e) {
            logger.error("Error fetching reports: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(500, "Error retrieving reports"));
        }
    }

    @GetMapping("/level/{levelId}")
    public ResponseEntity<ApiResponse<?>> getReportsByLevel(@PathVariable String levelId) {
        try {
            logger.info("Fetching reports for level: {}", levelId);

            List<ReportDTO> reports = reportService.getReportsByLevel(levelId);
            logger.info("Found {} reports for level: {}", reports.size(), levelId);

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(200, "Reports retrieved successfully", reports));
        } catch (Exception e) {
            logger.error("Error fetching reports by level: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(500, "Error retrieving reports"));
        }
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<ApiResponse<?>> getReportById(@PathVariable Long reportId) {
        try {
            logger.info("Fetching report with ID: {}", reportId);

            ReportDTO report = reportService.getReportById(reportId);

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(200, "Report retrieved successfully", report));
        } catch (IllegalArgumentException e) {
            logger.warn("Report not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(404, e.getMessage()));
        } catch (Exception e) {
            logger.error("Error fetching report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(500, "Error retrieving report"));
        }
    }
}

