package com.example.gateway.security;

import com.example.gateway.document.DocumentRecord;
import com.example.gateway.document.DocumentSharingPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class DocumentAccessPolicy {

    public Mono<Boolean> canRead(Authentication authentication, DocumentRecord document) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

        if (isAdmin || document.ownerUsername().equals(authentication.getName())) {
            return Mono.just(true);
        }

        String subjectTenant = tenant(authentication);
        boolean sameTenant = subjectTenant != null && subjectTenant.equals(document.tenantId());
        boolean tenantShared = document.sharingPolicy() == DocumentSharingPolicy.TENANT;

        return Mono.just(sameTenant && tenantShared);
    }

    private String tenant(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return jwtAuthenticationToken.getToken().getClaimAsString("tenant");
        }

        return null;
    }
}
