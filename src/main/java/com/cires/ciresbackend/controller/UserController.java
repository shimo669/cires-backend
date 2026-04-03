package com.cires.ciresbackend.controller;

import com.cires.ciresbackend.entity.User;
import com.cires.ciresbackend.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register/save") // Must match register.html action
    public String registerUser(@ModelAttribute User user) {
        // Use the registerCitizen method which has the encoding logic
        userService.registerCitizen(user);
        return "redirect:/login?success";
    }
}