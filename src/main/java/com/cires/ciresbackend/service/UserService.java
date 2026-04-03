package com.cires.ciresbackend.service;

import com.cires.ciresbackend.entity.Role;
import com.cires.ciresbackend.entity.User;
import com.cires.ciresbackend.repository.RoleRepository;
import com.cires.ciresbackend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void registerCitizen(User user) {
        try {
            logger.info("Starting registration for: {}", user.getUsername());

            // Encrypt password
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            
            // Set default CITIZEN role
            Role citizenRole = roleRepository.findByRoleName("CITIZEN")
                    .orElseGet(() -> {
                        Role role = new Role();
                        role.setRoleName("CITIZEN");
                        return roleRepository.save(role);
                    });
            user.setRole(citizenRole);

            userRepository.save(user);
            logger.info("Registration successful for: {}", user.getUsername());
        } catch (Exception e) {
            logger.error("Registration failed for: {}", user.getUsername(), e);
            throw e;
        }
    }
}