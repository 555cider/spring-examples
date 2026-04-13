package com.example.gateway.controller;

import java.util.Map;

import com.example.gateway.document.DocumentCatalog;
import com.example.gateway.document.DocumentRecord;
import com.example.gateway.security.DocumentAccessPolicy;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api")
public class AuthorizationApiController {

    private final DocumentCatalog documentCatalog;
    private final DocumentAccessPolicy documentAccessPolicy;

    public AuthorizationApiController(DocumentCatalog documentCatalog, DocumentAccessPolicy documentAccessPolicy) {
        this.documentCatalog = documentCatalog;
        this.documentAccessPolicy = documentAccessPolicy;
    }

    @GetMapping("/me")
    public Mono<Map<String, Object>> me(Authentication authentication) {
        java.util.LinkedHashMap<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("username", authentication.getName());
        response.put("tenant", tenant(authentication));
        response.put("authorities", authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .sorted()
                .toList());

        return Mono.just(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/reports")
    public Mono<Map<String, Object>> adminReports(Authentication authentication) {
        return Mono.just(Map.of(
                "report", "monthly-summary",
                "requestedBy", authentication.getName()
        ));
    }

    @GetMapping("/documents/{id}")
    public Mono<Map<String, Object>> document(@PathVariable String id, Authentication authentication) {
        return documentCatalog.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(NOT_FOUND)))
                .flatMap(document -> authorizeDocument(authentication, document));
    }

    private Mono<Map<String, Object>> authorizeDocument(Authentication authentication, DocumentRecord document) {
        return documentAccessPolicy.canRead(authentication, document)
                .flatMap(allowed -> {
                    if (!allowed) {
                        return Mono.error(new ResponseStatusException(FORBIDDEN));
                    }

                    return Mono.just(Map.of(
                            "id", document.id(),
                            "title", document.title(),
                            "ownerUsername", document.ownerUsername(),
                            "tenantId", document.tenantId(),
                            "sharingPolicy", document.sharingPolicy().name()
                    ));
                });
    }

    private String tenant(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return jwtAuthenticationToken.getToken().getClaimAsString("tenant");
        }

        return null;
    }
}
