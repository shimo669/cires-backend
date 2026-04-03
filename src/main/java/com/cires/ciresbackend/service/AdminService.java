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
        userRepository.save(user);
        return "User role updated successfully";
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