package com.example.gateway.controller;

import java.util.Map;

import reactor.core.publisher.Mono;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthorizationApiController {

    @GetMapping("/me")
    public Mono<Map<String, Object>> me(Authentication authentication) {
        return Mono.just(Map.of(
                "username", authentication.getName(),
                "authorities", authentication.getAuthorities().stream()
                        .map(authority -> authority.getAuthority())
                        .filter(authority -> !authority.startsWith("FACTOR_"))
                        .sorted()
                        .toList()
        ));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/reports")
    public Mono<Map<String, Object>> adminReports(Authentication authentication) {
        return Mono.just(Map.of(
                "report", "monthly-summary",
                "requestedBy", authentication.getName()
        ));
    }
}
