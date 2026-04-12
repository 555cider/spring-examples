package com.example.gateway.security;

import com.example.gateway.document.DocumentRecord;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class DocumentAccessPolicy {

    public Mono<Boolean> canRead(Authentication authentication, DocumentRecord document) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

        return Mono.just(isAdmin || document.ownerUsername().equals(authentication.getName()));
    }
}
