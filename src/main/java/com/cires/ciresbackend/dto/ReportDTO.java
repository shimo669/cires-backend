package com.cires.ciresbackend.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportDTO {
    private Long id;
    private String title;
    private String description;
    private String status;
    private String categoryName;
    private Long categoryId;
    private String reporterUsername;
    private Long reporterId;
    private String incidentLocationName;
    private Long incidentLocationId;
    private LocalDateTime createdAt;
    private LocalDateTime slaDeadline;
    private Boolean reporterConfirmationRequired;
    private Boolean reporterApproved;
    private Integer serviceRating;
    private String serviceComment;
    private LocalDateTime reporterConfirmedAt;
}
