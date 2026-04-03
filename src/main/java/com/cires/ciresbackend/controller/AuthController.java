package com.cires.ciresbackend.controller;

import com.cires.ciresbackend.dto.ApiResponse;
import com.cires.ciresbackend.dto.AuthResponse;
import com.cires.ciresbackend.dto.LoginRequest;
import com.cires.ciresbackend.dto.RegisterRequest;
import com.cires.ciresbackend.entity.Role;
import com.cires.ciresbackend.entity.User;
import com.cires.ciresbackend.repository.RoleRepository;
import com.cires.ciresbackend.repository.UserRepository;
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
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

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

        User savedUser = userRepository.save(user);
        logger.info("User '{}' registered successfully", request.getUsername());

        AuthResponse response = new AuthResponse();
        response.setUsername(savedUser.getUsername());
        response.setEmail(savedUser.getEmail());
        response.setNationalId(savedUser.getNationalId());
        response.setRole(citizenRole.getRoleName());

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
            Authentication authentication = authenticationManager.authenticate(
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

            logger.info("User '{}' logged in successfully", request.getUsername());
            return ResponseEntity.ok()
                    .body(new ApiResponse<>(200, "Login successful", response));

        } catch (AuthenticationException e) {
            logger.warn("Login failed for username: {}. Error: {}", request.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(401, "Invalid username or password"));
        }
    }
}