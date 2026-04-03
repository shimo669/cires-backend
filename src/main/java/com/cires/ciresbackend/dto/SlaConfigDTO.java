package com.cires.ciresbackend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SlaConfigDTO {
    private Long categoryId;
    private String levelType;
    private Integer durationHours;
}
