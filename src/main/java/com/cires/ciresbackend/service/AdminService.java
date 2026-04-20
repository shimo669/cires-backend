package com.cires.ciresbackend.service;

import com.cires.ciresbackend.dto.RoleUpdateDTO;
import com.cires.ciresbackend.dto.SlaConfigDTO;
import com.cires.ciresbackend.dto.ReportDTO;
import com.cires.ciresbackend.dto.UserResponseDTO;
import com.cires.ciresbackend.entity.*;
import com.cires.ciresbackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
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
    private final ReportRepository reportRepository;

    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream().map(this::toUserResponseDTO).collect(Collectors.toList());
    }

    @Transactional
    public String updateUserRole(Long userId, RoleUpdateDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Province existingProvince = user.getProvince();
        District existingDistrict = user.getDistrict();
        Sector existingSector = user.getSector();
        Cell existingCell = user.getCell();
        Village existingVillage = user.getVillage();

        String normalizedRoleName = normalizeRoleName(request.getRoleName());
        if (normalizedRoleName == null) {
            throw new RuntimeException("roleName is required");
        }

        Role role = roleRepository.findByRoleName(normalizedRoleName)
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setRoleName(normalizedRoleName);
                    return roleRepository.save(newRole);
                });

        user.setRole(role);

        User.UserLevelType levelType = resolveLevelType(request.getLevelType(), request.getLocationType(), normalizedRoleName, user.getLevelType());

        clearGeography(user);

        switch (levelType) {
            case PROVINCE_GOVERNOR -> {
                Province province = request.getLocationId() != null
                        ? provinceRepository.findById(requireLocationId(request.getLocationId()))
                        .orElseThrow(() -> new RuntimeException("Province not found"))
                        : existingProvince;
                if (province == null) {
                    throw new RuntimeException("Province not found");
                }
                user.setProvince(province);
                user.setLevelType(User.UserLevelType.PROVINCE_GOVERNOR);
            }
            case DISTRICT_MAYOR -> {
                District district = request.getLocationId() != null
                        ? districtRepository.findById(requireLocationId(request.getLocationId()))
                        .orElseThrow(() -> new RuntimeException("District not found"))
                        : existingDistrict;
                if (district == null) {
                    throw new RuntimeException("District not found");
                }
                user.setDistrict(district);
                user.setLevelType(User.UserLevelType.DISTRICT_MAYOR);
            }
            case SECTOR_LEADER -> {
                Sector sector = request.getLocationId() != null
                        ? sectorRepository.findById(requireLocationId(request.getLocationId()))
                        .orElseThrow(() -> new RuntimeException("Sector not found"))
                        : existingSector;
                if (sector == null) {
                    throw new RuntimeException("Sector not found");
                }
                user.setSector(sector);
                user.setLevelType(User.UserLevelType.SECTOR_LEADER);
            }
            case CELL_LEADER -> {
                Cell cell = request.getLocationId() != null
                        ? cellRepository.findById(requireLocationId(request.getLocationId()))
                        .orElseThrow(() -> new RuntimeException("Cell not found"))
                        : existingCell;
                if (cell == null) {
                    throw new RuntimeException("Cell not found");
                }
                user.setCell(cell);
                user.setLevelType(User.UserLevelType.CELL_LEADER);
            }
            case VILLAGE_LEADER -> {
                Village village = request.getLocationId() != null
                        ? villageRepository.findById(requireLocationId(request.getLocationId()))
                        .orElseThrow(() -> new RuntimeException("Village not found"))
                        : existingVillage;
                if (village == null) {
                    throw new RuntimeException("Village not found");
                }
                user.setVillage(village);
                user.setLevelType(User.UserLevelType.VILLAGE_LEADER);
            }
            case NATIONAL_ADMIN, CITIZEN -> user.setLevelType(levelType);
            default -> throw new RuntimeException("Unsupported level type");
        }

        userRepository.saveAndFlush(user);
        return "User role updated successfully";
    }

    public List<ReportDTO> getAllReports() {
        return reportRepository.findAll().stream().map(this::toReportDTO).collect(Collectors.toList());
    }

    private Long requireLocationId(Long locationId) {
        if (locationId == null) {
            throw new RuntimeException("locationId is required for this level type");
        }
        return locationId;
    }

    private UserResponseDTO toUserResponseDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole() != null ? toDisplayRoleName(user.getRole().getRoleName()) : "NO_ROLE");
        dto.setLevelType(user.getLevelType() != null ? user.getLevelType().name() : "CITIZEN");
        dto.setLocationId(resolveLocationId(user));
        dto.setLocationName(resolveLocationName(user));
        dto.setFullRwandanAddress(buildFullRwandanAddress(user));
        return dto;
    }

    private ReportDTO toReportDTO(Report report) {
        ReportDTO dto = new ReportDTO();
        dto.setId(report.getId());
        dto.setTitle(report.getTitle());
        dto.setDescription(report.getDescription());
        dto.setStatus(report.getStatus());
        dto.setCategoryId(report.getCategory().getId());
        dto.setCategoryName(report.getCategory().getCategoryName());
        dto.setReporterId(report.getReporter().getId());
        dto.setReporterUsername(report.getReporter().getUsername());
        dto.setIncidentLocationId(report.getIncidentVillage().getId());
        dto.setIncidentLocationName(report.getIncidentVillage().getName() + " Village");
        dto.setCreatedAt(report.getCreatedAt());
        dto.setSlaDeadline(report.getSlaDeadline());
        return dto;
    }

    private String normalizeRoleName(String roleName) {
        if (roleName == null || roleName.trim().isEmpty()) {
            return null;
        }
        String normalized = roleName.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized.substring("ROLE_".length()) : normalized;
    }

    private String toDisplayRoleName(String roleName) {
        if (roleName == null || roleName.trim().isEmpty()) {
            return "NO_ROLE";
        }
        String normalized = roleName.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }

    private User.UserLevelType resolveLevelType(String requestedLevelType, String requestedLocationType, String roleName, User.UserLevelType currentLevel) {
        if (requestedLevelType != null && !requestedLevelType.trim().isEmpty()) {
            try {
                return User.UserLevelType.valueOf(requestedLevelType.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new RuntimeException("Invalid level type");
            }
        }

        if (requestedLocationType != null && !requestedLocationType.trim().isEmpty()) {
            return switch (requestedLocationType.trim().toUpperCase(Locale.ROOT)) {
                case "PROVINCE" -> User.UserLevelType.PROVINCE_GOVERNOR;
                case "DISTRICT" -> User.UserLevelType.DISTRICT_MAYOR;
                case "SECTOR" -> User.UserLevelType.SECTOR_LEADER;
                case "CELL" -> User.UserLevelType.CELL_LEADER;
                case "VILLAGE" -> User.UserLevelType.VILLAGE_LEADER;
                default -> throw new RuntimeException("Invalid location type");
            };
        }

        if (roleName != null) {
            return switch (roleName) {
                case "ADMIN" -> User.UserLevelType.NATIONAL_ADMIN;
                case "CITIZEN" -> User.UserLevelType.CITIZEN;
                case "LEADER" -> currentLevel != null ? currentLevel : throwMissingLeaderLevel();
                default -> currentLevel != null ? currentLevel : User.UserLevelType.CITIZEN;
            };
        }

        return currentLevel != null ? currentLevel : User.UserLevelType.CITIZEN;
    }

    private User.UserLevelType throwMissingLeaderLevel() {
        throw new RuntimeException("Level type is required for leader accounts");
    }

    private void clearGeography(User user) {
        user.setProvince(null);
        user.setDistrict(null);
        user.setSector(null);
        user.setCell(null);
        user.setVillage(null);
    }

    private Long resolveLocationId(User user) {
        if (user.getVillage() != null) return user.getVillage().getId();
        if (user.getCell() != null) return user.getCell().getId();
        if (user.getSector() != null) return user.getSector().getId();
        if (user.getDistrict() != null) return user.getDistrict().getId();
        if (user.getProvince() != null) return user.getProvince().getId();
        return null;
    }

    private String resolveLocationName(User user) {
        if (user.getVillage() != null) return user.getVillage().getName();
        if (user.getCell() != null) return user.getCell().getName();
        if (user.getSector() != null) return user.getSector().getName();
        if (user.getDistrict() != null) return user.getDistrict().getName();
        if (user.getProvince() != null) return user.getProvince().getName();
        return "N/A";
    }

    private String buildFullRwandanAddress(User user) {
        List<String> parts = new java.util.ArrayList<>();

        if (user.getProvince() != null) {
            parts.add(user.getProvince().getName() + " Province");
        }
        if (user.getDistrict() != null) {
            parts.add(user.getDistrict().getName() + " District");
        }
        if (user.getSector() != null) {
            parts.add(user.getSector().getName() + " Sector");
        }
        if (user.getCell() != null) {
            parts.add(user.getCell().getName() + " Cell");
        }
        if (user.getVillage() != null) {
            parts.add(user.getVillage().getName() + " Village");
        }

        return parts.isEmpty() ? "N/A" : String.join(", ", parts);
    }

    @Transactional
    public String createSlaConfig(SlaConfigDTO request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (request.getLevelType() == null || request.getLevelType().trim().isEmpty()) {
            throw new RuntimeException("SLA level type is required");
        }

        GovernmentLevelType levelType;
        try {
            levelType = GovernmentLevelType.valueOf(request.getLevelType().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Invalid SLA level type");
        }

        if (request.getDurationHours() == null || request.getDurationHours() <= 0) {
            throw new RuntimeException("SLA duration must be greater than 0");
        }

        SlaConfig slaConfig = slaConfigRepository.findByCategoryIdAndLevelType(category.getId(), levelType)
                .orElseGet(SlaConfig::new);
        slaConfig.setCategory(category);
        slaConfig.setLevelType(levelType);
        slaConfig.setDurationHours(request.getDurationHours());

        slaConfigRepository.save(slaConfig);

        return "SLA Configuration saved successfully!";
    }
}