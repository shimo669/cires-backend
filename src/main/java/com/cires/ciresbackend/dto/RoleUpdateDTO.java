package com.cires.ciresbackend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoleUpdateDTO {
    private String roleName;
    private String levelType;
    private Long locationId;
}
