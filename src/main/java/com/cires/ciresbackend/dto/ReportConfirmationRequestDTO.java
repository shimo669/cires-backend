package com.cires.ciresbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportConfirmationRequestDTO {
    private Boolean approved;
    private Integer rating;
    private String comment;
}

