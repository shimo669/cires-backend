package com.cires.ciresbackend.service;

import com.cires.ciresbackend.dto.RoleUpdateDTO;
import com.cires.ciresbackend.dto.SlaConfigDTO;
import com.cires.ciresbackend.dto.UserResponseDTO;
import com.cires.ciresbackend.entity.*;
import com.cires.ciresbackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SlaConfigRepository slaConfigRepository;
    private final CategoryRepository categoryRepository;
    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;
    private final SectorRepository sectorRepository;
    private final CellRepository cellRepository;
    private final VillageRepository villageRepository;

    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream().map(user -> {
            UserResponseDTO dto = new UserResponseDTO();
            dto.setId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setEmail(user.getEmail());
            dto.setRole(user.getRole() != null ? user.getRole().getRoleName() : "NO_ROLE");

            // Map the new UserLevelType safely
            dto.setLevelType(user.getLevelType() != null ? user.getLevelType().name() : "CITIZEN");
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional
    public String updateUserRole(Long userId, RoleUpdateDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Role role = roleRepository.findByRoleName(request.getRoleName())
                .orElseThrow(() -> new RuntimeException("Role not found"));

        user.setRole(role);

        // Always clear previous geography before assigning the new leadership scope.
        user.setProvince(null);
        user.setDistrict(null);
        user.setSector(null);
        user.setCell(null);
        user.setVillage(null);

        if (request.getLevelType() == null || request.getLevelType().trim().isEmpty()) {
            user.setLevelType(User.UserLevelType.CITIZEN);
            userRepository.save(user);
            return "User role updated successfully";
        }

        User.UserLevelType levelType;
        try {
            levelType = User.UserLevelType.valueOf(request.getLevelType().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Invalid level type");
        }

        switch (levelType) {
            case PROVINCE_GOVERNOR -> {
                Province province = provinceRepository.findById(requireLocationId(request.getLocationId()))
                        .orElseThrow(() -> new RuntimeException("Province not found"));
                user.setProvince(province);
                user.setLevelType(User.UserLevelType.PROVINCE_GOVERNOR);
            }
            case DISTRICT_MAYOR -> {
                District district = districtRepository.findById(requireLocationId(request.getLocationId()))
                        .orElseThrow(() -> new RuntimeException("District not found"));
                user.setDistrict(district);
                user.setLevelType(User.UserLevelType.DISTRICT_MAYOR);
            }
            case SECTOR_LEADER -> {
                Sector sector = sectorRepository.findById(requireLocationId(request.getLocationId()))
                        .orElseThrow(() -> new RuntimeException("Sector not found"));
                user.setSector(sector);
                user.setLevelType(User.UserLevelType.SECTOR_LEADER);
            }
            case CELL_LEADER -> {
                Cell cell = cellRepository.findById(requireLocationId(request.getLocationId()))
                        .orElseThrow(() -> new RuntimeException("Cell not found"));
                user.setCell(cell);
                user.setLevelType(User.UserLevelType.CELL_LEADER);
            }
            case VILLAGE_LEADER -> {
                Village village = villageRepository.findById(requireLocationId(request.getLocationId()))
                        .orElseThrow(() -> new RuntimeException("Village not found"));
                user.setVillage(village);
                user.setLevelType(User.UserLevelType.VILLAGE_LEADER);
            }
            case NATIONAL_ADMIN, CITIZEN -> user.setLevelType(levelType);
            default -> throw new RuntimeException("Unsupported level type");
        }

        userRepository.save(user);
        return "User role updated successfully";
    }

    private Long requireLocationId(Long locationId) {
        if (locationId == null) {
            throw new RuntimeException("locationId is required for this level type");
        }
        return locationId;
    }

    @Transactional
    public String createSlaConfig(SlaConfigDTO request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        SlaConfig slaConfig = new SlaConfig();
        slaConfig.setCategory(category);
        slaConfig.setDurationHours(request.getDurationHours());

        // SLA Configs will now be tied to the EscalationLevel Enum or Category directly
        // rather than the old GovernmentLevel table.
        slaConfigRepository.save(slaConfig);

        return "SLA Configuration saved successfully!";
    }
}