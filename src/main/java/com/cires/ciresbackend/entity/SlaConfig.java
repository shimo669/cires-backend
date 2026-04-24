package com.cires.ciresbackend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sla_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SlaConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "level_type", nullable = false)
    private GovernmentLevelType levelType;

    @Column(name = "duration_hours", nullable = false)
    private Integer durationHours;
}
