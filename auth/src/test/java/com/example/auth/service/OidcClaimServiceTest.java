package com.example.auth.service;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.profiles.active=test",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.discovery.register-enabled=false"
})
@ActiveProfiles("test")
class OidcClaimServiceTest {

    @Autowired
    private OidcClaimService oidcClaimService;

    @Autowired
    private JdbcUserDetailsService userDetailsService;

    @Test
    void userInfoClaimsIncludeRequestedStandardClaimsAndRoles() {
        Map<String, Object> userInfoClaims = oidcClaimService.userInfoClaims("admin", Set.of("openid", "profile", "email"));

        assertThat(userInfoClaims)
                .containsEntry("sub", "admin")
                .containsEntry("preferred_username", "admin")
                .containsEntry("email", "admin@example.com")
                .containsEntry("email_verified", true)
                .containsKey("updated_at");
        assertThat(userInfoClaims.get("roles")).isEqualTo(java.util.List.of("ROLE_ADMIN"));
    }

    @Test
    void userInfoClaimsFilterOutUnrequestedStandardClaims() {
        Map<String, Object> userInfoClaims = oidcClaimService.userInfoClaims("admin", Set.of("openid"));

        assertThat(userInfoClaims)
                .containsEntry("sub", "admin")
                .containsEntry("roles", java.util.List.of("ROLE_ADMIN"))
                .doesNotContainKeys("email", "email_verified", "preferred_username", "updated_at");
    }

    private Authentication principal(String username) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        return UsernamePasswordAuthenticationToken.authenticated(
                userDetails,
                "N/A",
                userDetails.getAuthorities()
        );
    }
}
