package com.cires.ciresbackend.controller;

import com.cires.ciresbackend.dto.RoleUpdateDTO;
import com.cires.ciresbackend.dto.SlaConfigDTO;
import com.cires.ciresbackend.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin("*")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PutMapping("/users/{userId}/role")
    public ResponseEntity<?> updateRole(@PathVariable Long userId, @RequestBody RoleUpdateDTO roleName) {
        return ResponseEntity.ok(adminService.updateUserRole(userId, roleName));
    }

    @PostMapping("/sla-configs")
    public ResponseEntity<?> createSla(@RequestBody SlaConfigDTO config) {
        return ResponseEntity.ok(adminService.createSlaConfig(config));
    }
}