package com.cires.ciresbackend.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class CustomSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        var roles = authentication.getAuthorities();
        String targetUrl = "/login?error";

        for (var role : roles) {
            if (role.getAuthority().equals("ROLE_ADMIN")) {
                targetUrl = "/admin/dashboard";
                break;
            } else if (role.getAuthority().equals("ROLE_LEADER")) {
                targetUrl = "/leader/dashboard";
                break;
            } else if (role.getAuthority().equals("ROLE_CITIZEN")) {
                targetUrl = "/citizen/dashboard";
                break;
            }
        }
        response.sendRedirect(targetUrl);
    }
}