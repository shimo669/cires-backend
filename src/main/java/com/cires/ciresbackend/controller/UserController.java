package com.cires.ciresbackend.controller;

import com.cires.ciresbackend.entity.User;
import com.cires.ciresbackend.entity.Village;
import com.cires.ciresbackend.repository.VillageRepository;
import com.cires.ciresbackend.service.UserService;
import org.springframework.web.bind.annotation.*;

// Changed from @Controller to @RestController to work with your React Frontend
@RestController
// Updated with /api prefix to align with SecurityConfig
@RequestMapping("/api/users")
@CrossOrigin(origins = {"https://cires-frontend.onrender.com", "http://localhost:5173"}, allowCredentials = "true")
public class UserController {

    private final UserService userService;
    private final VillageRepository villageRepository;

    public UserController(UserService userService, VillageRepository villageRepository) {
        this.userService = userService;
        this.villageRepository = villageRepository;
    }

    // Updated path to /api/users/register/save
    @PostMapping("/register/save")
    public String registerUser(@RequestBody User user, @RequestParam(required = false) Long locationId) {
        if (locationId != null && user.getVillage() == null) {
            Village village = villageRepository.findById(locationId)
                    .orElseThrow(() -> new IllegalArgumentException("Village not found"));
            assignGeographyFromVillage(user, village);
        }

        userService.registerCitizen(user);

        // Note: With @RestController, "redirect:" doesn't work the same way as @Controller.
        // Usually, the Frontend handles the redirect after receiving a 200 OK.
        return "User registered successfully";
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