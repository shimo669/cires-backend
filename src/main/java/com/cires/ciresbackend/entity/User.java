package com.cires.ciresbackend.entity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(unique = true, nullable = false) private String username;
    @Column(unique = true, nullable = false) private String email;
    @Column(nullable = false) private String password;
    @Column(name = "nationalId", unique = true, nullable = false) private String nationalId;

    @ManyToOne @JoinColumn(name = "role_id")
    private Role role;

    public enum UserLevelType { CITIZEN, VILLAGE_LEADER, CELL_LEADER, SECTOR_LEADER, DISTRICT_MAYOR, PROVINCE_GOVERNOR, NATIONAL_ADMIN }
    @Enumerated(EnumType.STRING)
    private UserLevelType levelType = UserLevelType.CITIZEN;

    // Strict Geography Links
    @ManyToOne @JoinColumn(name = "province_id") private Province province;
    @ManyToOne @JoinColumn(name = "district_id") private District district;
    @ManyToOne @JoinColumn(name = "sector_id") private Sector sector;
    @ManyToOne @JoinColumn(name = "cell_id") private Cell cell;
    @ManyToOne @JoinColumn(name = "village_id") private Village village;
}