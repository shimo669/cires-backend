package com.cires.ciresbackend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class HistoryResponseDTO {
    private String action;
    private String notes;
    private LocalDateTime timestamp;
    private String actedBy;
}