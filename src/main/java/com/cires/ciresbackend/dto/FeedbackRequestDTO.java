package com.cires.ciresbackend.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackRequestDTO {
    private Integer rating;
    private String comment;
}

