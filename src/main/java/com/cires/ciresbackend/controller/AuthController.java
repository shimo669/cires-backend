package com.cires.ciresbackend.controller;

import com.cires.ciresbackend.dto.ApiResponse;
import com.cires.ciresbackend.dto.AuthResponse;
import com.cires.ciresbackend.dto.LoginRequest;
import com.cires.ciresbackend.dto.RegisterRequest;
import com.cires.ciresbackend.entity.Role;
import com.cires.ciresbackend.entity.Village;
import com.cires.ciresbackend.entity.User;
import com.cires.ciresbackend.repository.RoleRepository;
import com.cires.ciresbackend.repository.UserRepository;
import com.cires.ciresbackend.repository.VillageRepository;
import com.cires.ciresbackend.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private VillageRepository villageRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<?>> register(@RequestBody RegisterRequest request) {
        logger.info("Registration attempt for username: {}", request.getUsername());

        // Validate input
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(400, "Username is required"));
        }

        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(400, "Email is required"));
        }

        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(400, "Password is required"));
        }

        if (request.getNationalId() == null || request.getNationalId().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(400, "National ID is required"));
        }

        // Check if user already exists
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            logger.warn("Registration failed: Username '{}' already exists", request.getUsername());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(400, "Username already exists"));
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            logger.warn("Registration failed: Email '{}' already exists", request.getEmail());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(400, "Email already exists"));
        }

        if (userRepository.findByNationalId(request.getNationalId()).isPresent()) {
            logger.warn("Registration failed: National ID '{}' already exists", request.getNationalId());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(400, "National ID already exists"));
        }

        // Get or create default CITIZEN role
        Role citizenRole = roleRepository.findByRoleName("CITIZEN")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setRoleName("CITIZEN");
                    return roleRepository.save(role);
                });

        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNationalId(request.getNationalId());
        user.setRole(citizenRole);

        if (request.getLocationId() != null) {
            Village village = villageRepository.findById(request.getLocationId())
                    .orElseThrow(() -> new IllegalArgumentException("Village not found"));
            assignGeographyFromVillage(user, village);
        }

        User savedUser = userRepository.save(user);
        logger.info("User '{}' registered successfully", request.getUsername());

        AuthResponse response = new AuthResponse();
        response.setUsername(savedUser.getUsername());
        response.setEmail(savedUser.getEmail());
        response.setNationalId(savedUser.getNationalId());
        response.setRole(citizenRole.getRoleName());
        applyLocationResponse(response, savedUser);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(201, "User registered successfully", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<?>> login(@RequestBody LoginRequest request) {
        logger.info("Login attempt for username: {}", request.getUsername());

        // Validate input
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(400, "Username is required"));
        }

        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(400, "Password is required"));
        }

        try {
            // Authenticate user
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            // Get user details
            Optional<User> userOpt = userRepository.findByUsername(request.getUsername());
            if (userOpt.isEmpty()) {
                logger.error("User '{}' not found after authentication", request.getUsername());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(401, "Authentication failed"));
            }

            User user = userOpt.get();
            String role = user.getRole().getRoleName();
            
            // Generate JWT token
            String token = jwtTokenProvider.generateTokenFromUsername(request.getUsername(), role);

            AuthResponse response = new AuthResponse();
            response.setToken(token);
            response.setUsername(user.getUsername());
            response.setEmail(user.getEmail());
            response.setNationalId(user.getNationalId());
            response.setRole(role);
            applyLocationResponse(response, user);

            logger.info("User '{}' logged in successfully", request.getUsername());
            return ResponseEntity.ok()
                    .body(new ApiResponse<>(200, "Login successful", response));

        } catch (AuthenticationException e) {
            logger.warn("Login failed for username: {}. Error: {}", request.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(401, "Invalid username or password"));
        } catch (Exception e) {
            logger.error("Unexpected error during login for user: {}", request.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(500, "An unexpected server error occurred: " + e.getMessage()));
        }
    }

    private void applyLocationResponse(AuthResponse response, User user) {
        if (user.getVillage() != null) {
            response.setLocationId(user.getVillage().getId());
            response.setLocationName(user.getVillage().getName());
            response.setFullRwandanAddress(buildFullAddress(user));
            return;
        }
        if (user.getCell() != null) {
            response.setLocationId(user.getCell().getId());
            response.setLocationName(user.getCell().getName());
            response.setFullRwandanAddress(buildFullAddress(user));
            return;
        }
        if (user.getSector() != null) {
            response.setLocationId(user.getSector().getId());
            response.setLocationName(user.getSector().getName());
            response.setFullRwandanAddress(buildFullAddress(user));
            return;
        }
        if (user.getDistrict() != null) {
            response.setLocationId(user.getDistrict().getId());
            response.setLocationName(user.getDistrict().getName());
            response.setFullRwandanAddress(buildFullAddress(user));
            return;
        }
        if (user.getProvince() != null) {
            response.setLocationId(user.getProvince().getId());
            response.setLocationName(user.getProvince().getName());
            response.setFullRwandanAddress(buildFullAddress(user));
            return;
        }

        response.setLocationName("N/A");
        response.setFullRwandanAddress("N/A");
    }

    private String buildFullAddress(User user) {
        StringBuilder address = new StringBuilder();

        if (user.getProvince() != null) {
            address.append(user.getProvince().getName()).append(" Province");
        }
        if (user.getDistrict() != null) {
            appendPart(address, user.getDistrict().getName() + " District");
        }
        if (user.getSector() != null) {
            appendPart(address, user.getSector().getName() + " Sector");
        }
        if (user.getCell() != null) {
            appendPart(address, user.getCell().getName() + " Cell");
        }
        if (user.getVillage() != null) {
            appendPart(address, user.getVillage().getName() + " Village");
        }

        return address.length() == 0 ? "N/A" : address.toString();
    }

    private void appendPart(StringBuilder address, String part) {
        if (address.length() > 0) {
            address.append(", ");
        }
        address.append(part);
    }

    private void assignGeographyFromVillage(User user, Village village) {
        user.setVillage(village);
        if (village.getCell() != null) {
            user.setCell(village.getCell());
            if (village.getCell().getSector() != null) {
                user.setSector(village.getCell().getSector());
                if (village.getCell().getSector().getDistrict() != null) {
                    user.setDistrict(village.getCell().getSector().getDistrict());
                    if (village.getCell().getSector().getDistrict().getProvince() != null) {
                        user.setProvince(village.getCell().getSector().getDistrict().getProvince());
                    }
                }
            }
        }
    }
}