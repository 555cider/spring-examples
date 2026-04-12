package com.example.client.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ClientController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal OidcUser oidcUser, Model model) {
        model.addAttribute("username", username(oidcUser));
        model.addAttribute("email", oidcUser.getClaimAsString("email"));
        model.addAttribute("roles", roles(oidcUser));
        return "profile";
    }

    private String username(OidcUser oidcUser) {
        String preferredUsername = oidcUser.getClaimAsString("preferred_username");
        return preferredUsername != null ? preferredUsername : oidcUser.getName();
    }

    private List<String> roles(OidcUser oidcUser) {
        List<String> roles = oidcUser.getClaimAsStringList("roles");
        return roles == null ? List.of() : roles;
    }
}
