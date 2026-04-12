package com.example.auth.service;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.example.auth.domain.User;
import com.example.auth.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.stereotype.Service;

@Service
public class OidcClaimService {

    private final UserRepository userRepository;

    public OidcClaimService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<String> roleClaims(Authentication principal) {
        return principal.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .distinct()
                .sorted()
                .toList();
    }

    public Map<String, Object> idTokenClaims(Authentication principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + principal.getName()));

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", user.getUsername());
        claims.put("preferred_username", user.getUsername());
        claims.put("email", user.getEmail());
        claims.put("email_verified", Boolean.TRUE);

        OffsetDateTime updatedAt = user.getUpdatedAt() != null ? user.getUpdatedAt() : user.getCreatedAt();
        if (updatedAt != null) {
            claims.put("updated_at", updatedAt.toEpochSecond());
        }

        claims.put("roles", roleClaims(principal));
        return claims;
    }

    public Map<String, Object> userInfoClaims(Map<String, Object> idTokenClaims, Set<String> scopes) {
        Map<String, Object> claims = new LinkedHashMap<>();
        copyIfPresent(idTokenClaims, claims, "sub");
        copyIfPresent(idTokenClaims, claims, "roles");

        Set<String> requestedScopes = scopes == null ? Set.of() : scopes;
        if (requestedScopes.contains(OidcScopes.PROFILE)) {
            copyIfPresent(idTokenClaims, claims, "preferred_username");
            copyIfPresent(idTokenClaims, claims, "updated_at");
        }
        if (requestedScopes.contains(OidcScopes.EMAIL)) {
            copyIfPresent(idTokenClaims, claims, "email");
            copyIfPresent(idTokenClaims, claims, "email_verified");
        }

        return claims;
    }

    private void copyIfPresent(Map<String, Object> source, Map<String, Object> target, String claimName) {
        if (source.containsKey(claimName)) {
            target.put(claimName, source.get(claimName));
        }
    }
}
