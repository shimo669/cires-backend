package com.cires.ciresbackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AutoFixedSlaTimerDTO {
    private Long timerId;
    private Long reportId;
    private String reportStatus;
    private String escalationLevel;
    private String levelType;
    private LocalDateTime deadline;
    private LocalDateTime completedAt;
    private LocalDateTime autoFixedAt;
}

