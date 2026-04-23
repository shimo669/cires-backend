package com.cires.ciresbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "sla_timer",
        indexes = {
                @Index(name = "idx_sla_timer_status_deadline", columnList = "status,deadline"),
                @Index(name = "idx_sla_timer_auto_fixed", columnList = "auto_fixed_by_scheduler,auto_fixed_at")
        }
)
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

    @Column(name = "auto_fixed_by_scheduler", nullable = false)
    private Boolean autoFixedByScheduler;

    @Column(name = "auto_fixed_at")
    private LocalDateTime autoFixedAt;

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
        if (autoFixedByScheduler == null) {
            autoFixedByScheduler = false;
        }
    }

    public enum SlaTimerStatus {
        ACTIVE, COMPLETED, BREACHED
    }
}
