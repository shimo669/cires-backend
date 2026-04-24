package com.cires.ciresbackend.controller;

import com.cires.ciresbackend.dto.ApiResponse;
import com.cires.ciresbackend.dto.ReportConfirmationRequestDTO;
import com.cires.ciresbackend.service.ReportActionService;
import com.cires.ciresbackend.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportActionController {

    private final ReportActionService reportActionService;
    private final ReportService reportService;

    @PutMapping("/{reportId}/resolve")
    @PreAuthorize("hasRole('LEADER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> resolveReport(@PathVariable Long reportId) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            reportActionService.resolveReport(reportId, username);
            return ResponseEntity.ok(new ApiResponse<>(200, "Report marked as solved and waiting for reporter confirmation"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(404, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(500, "Error resolving report"));
        }
    }

    @PutMapping("/{reportId}/escalate")
    @PreAuthorize("hasRole('LEADER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> escalateReport(@PathVariable Long reportId) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            reportService.escalateReport(reportId, username);
            return ResponseEntity.ok(new ApiResponse<>(200, "Report escalated successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(404, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(500, "Error escalating report"));
        }
    }

    @PutMapping("/{reportId}/confirm")
    @PreAuthorize("hasAnyRole('CITIZEN','LEADER','ADMIN')")
    public ResponseEntity<ApiResponse<?>> confirmResolution(@PathVariable Long reportId,
                                                            @RequestBody ReportConfirmationRequestDTO request) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            reportService.confirmResolution(reportId, request, username);
            return ResponseEntity.ok(new ApiResponse<>(200, "Reporter confirmation saved successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(400, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(500, "Error confirming report resolution"));
        }
    }
}
