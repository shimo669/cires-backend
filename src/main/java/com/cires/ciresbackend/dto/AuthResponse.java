package com.cires.ciresbackend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String username;
    private String role;
    private String email;
    private String nationalId;
    private Long locationId;
    private String locationName;
    private String fullRwandanAddress;
}

