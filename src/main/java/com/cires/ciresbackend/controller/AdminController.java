package com.cires.ciresbackend.controller;

import com.cires.ciresbackend.dto.RoleUpdateDTO;
import com.cires.ciresbackend.dto.SlaConfigDTO;
import com.cires.ciresbackend.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
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

    @GetMapping("/reports")
    public ResponseEntity<?> getAllReports() {
        return ResponseEntity.ok(adminService.getAllReports());
    }

    @GetMapping("/sla-timers/auto-fixed")
    public ResponseEntity<?> getAutoFixedSlaTimers(@RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(adminService.getAutoFixedSlaTimers(limit));
    }
}