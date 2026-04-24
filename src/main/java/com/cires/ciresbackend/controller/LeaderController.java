package com.cires.ciresbackend.controller;

import com.cires.ciresbackend.dto.ApiResponse;
import com.cires.ciresbackend.dto.PagedResponseDTO;
import com.cires.ciresbackend.dto.ReportDTO;
import com.cires.ciresbackend.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/leader")
@RequiredArgsConstructor
@CrossOrigin("*")
public class LeaderController {

    private final ReportService reportService;

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<?>> getVisibleReports(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        // Backward compatibility: old clients expect data to be a plain list.
        if (page == null || size == null) {
            return ResponseEntity.ok(new ApiResponse<>(200, "Reports retrieved successfully", reportService.getVisibleReportsForUser(username)));
        }

        Page<ReportDTO> reportPage = reportService.getVisibleReportsPageForUser(username, page, size);

        PagedResponseDTO<ReportDTO> payload = new PagedResponseDTO<>();
        payload.setItems(reportPage.getContent());
        payload.setPage(reportPage.getNumber());
        payload.setSize(reportPage.getSize());
        payload.setTotalElements(reportPage.getTotalElements());
        payload.setTotalPages(reportPage.getTotalPages());
        payload.setLast(reportPage.isLast());

        return ResponseEntity.ok(new ApiResponse<>(200, "Reports retrieved successfully", payload));
    }

    @PutMapping("/reports/{reportId}/resolve")
    public ResponseEntity<ApiResponse<?>> resolveReport(@PathVariable Long reportId) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            reportService.resolveReport(reportId, username);
            return ResponseEntity.ok(new ApiResponse<>(200, "Report marked as solved and waiting for reporter confirmation"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(400, e.getMessage()));
        }
    }

    @PutMapping("/reports/{reportId}/escalate")
    public ResponseEntity<ApiResponse<?>> escalateReport(@PathVariable Long reportId) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            reportService.escalateReport(reportId, username);
            return ResponseEntity.ok(new ApiResponse<>(200, "Report escalated successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(400, e.getMessage()));
        }
    }
}

