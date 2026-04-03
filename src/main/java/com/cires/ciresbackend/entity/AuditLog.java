package com.cires.ciresbackend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(name = "user_id")
    private User user;

    private String action; // This must match the Repository method name

    @Column(columnDefinition = "TEXT")
    private String description;

    private String ipAddress;
    private LocalDateTime timestamp = LocalDateTime.now();
}