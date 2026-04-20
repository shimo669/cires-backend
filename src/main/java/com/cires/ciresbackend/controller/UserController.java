package com.cires.ciresbackend.controller;

import com.cires.ciresbackend.entity.User;
import com.cires.ciresbackend.entity.Village;
import com.cires.ciresbackend.repository.VillageRepository;
import com.cires.ciresbackend.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class UserController {

    private final UserService userService;
    private final VillageRepository villageRepository;

    public UserController(UserService userService, VillageRepository villageRepository) {
        this.userService = userService;
        this.villageRepository = villageRepository;
    }

    @PostMapping("/register/save") // Must match register.html action
    public String registerUser(@ModelAttribute User user, @RequestParam(required = false) Long locationId) {
        if (locationId != null && user.getVillage() == null) {
            Village village = villageRepository.findById(locationId)
                    .orElseThrow(() -> new IllegalArgumentException("Village not found"));
            assignGeographyFromVillage(user, village);
        }

        // Use the registerCitizen method which has the encoding logic
        userService.registerCitizen(user);
        return "redirect:/login?success";
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