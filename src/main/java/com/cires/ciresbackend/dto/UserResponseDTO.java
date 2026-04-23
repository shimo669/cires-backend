package com.cires.ciresbackend.dto;
import lombok.Data;

@Data
public class UserResponseDTO {
    private Long id;
    private String username;
    private String email;
    private String nationalId;
    private String role;
    private String levelType;
    private Long locationId;
    private String locationName;
    private String fullRwandanAddress;
}