package com.cires.ciresbackend.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Report {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    @Column(nullable = false) private String title;
    @Column(columnDefinition = "TEXT", nullable = false) private String description;
    @Column(nullable = false) private String specificAddress;
    @Column(nullable = false) private String status = "PENDING";

    @ManyToOne @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    // Locks the ticket exactly to the village where the incident happened
    @ManyToOne @JoinColumn(name = "incident_village_id", nullable = false)
    private Village incidentVillage;

    // Tracks exactly which leader currently has ownership of the ticket
    public enum EscalationLevel { AT_VILLAGE, AT_CELL, AT_SECTOR, AT_DISTRICT, AT_PROVINCE, AT_NATIONAL }
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EscalationLevel currentEscalationLevel = EscalationLevel.AT_VILLAGE;

    private LocalDateTime createdAt;
    private LocalDateTime slaDeadline;

    @PrePersist protected void onCreate() { this.createdAt = LocalDateTime.now(); }
}