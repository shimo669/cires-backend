package com.cires.ciresbackend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "complaints")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Complaint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private String status; // Default could be "PENDING"

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User citizen; // Links the complaint to the user who reported it
}