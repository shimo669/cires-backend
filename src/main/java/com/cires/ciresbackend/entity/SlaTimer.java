package com.cires.ciresbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sla_timer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SlaTimer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

    @Enumerated(EnumType.STRING)
    @Column(name = "level_type", nullable = false)
    private GovernmentLevelType levelType;

    @Column(name = "start_time", nullable = false, updatable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime deadline;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SlaTimerStatus status;

    @Column(nullable = false)
    private Boolean breached;

    @PrePersist
    protected void onCreate() {
        if (startTime == null) {
            startTime = LocalDateTime.now();
        }
        if (status == null) {
            status = SlaTimerStatus.ACTIVE;
        }
        if (breached == null) {
            breached = false;
        }
    }

    public enum SlaTimerStatus {
        ACTIVE, COMPLETED, BREACHED
    }
}
