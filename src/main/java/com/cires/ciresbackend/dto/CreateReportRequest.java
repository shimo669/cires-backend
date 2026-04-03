package com.cires.ciresbackend.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateReportRequest {
    private String title;
    private String description;
    private Long categoryId;
    private Long incidentLocationId;
    private LocalDateTime slaDeadline;
}
