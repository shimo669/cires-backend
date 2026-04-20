package com.cires.ciresbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "report_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acted_by_id")
    private User actedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_level_type")
    private GovernmentLevelType fromLevelType;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_level_type")
    private GovernmentLevelType toLevelType;

    @Column(nullable = false)
    private String action;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "action_timestamp", nullable = false, updatable = false)
    private LocalDateTime actionTimestamp;

    @PrePersist
    protected void onCreate() {
        if (actionTimestamp == null) {
            actionTimestamp = LocalDateTime.now();
        }
    }
}
