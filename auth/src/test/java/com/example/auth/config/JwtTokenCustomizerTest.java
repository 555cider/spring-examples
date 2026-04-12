package com.example.auth.config;

import com.example.auth.service.JdbcUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.profiles.active=test",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.discovery.register-enabled=false"
})
@ActiveProfiles("test")
class JwtTokenCustomizerTest {

    @Autowired
    private OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer;

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Autowired
    private JdbcUserDetailsService userDetailsService;

    @Test
    void addsRolesClaimToAccessTokens() {
        RegisteredClient registeredClient = registeredClientRepository.findByClientId("client_id_1");
        UserDetails userDetails = userDetailsService.loadUserByUsername("admin");
        Authentication principal = UsernamePasswordAuthenticationToken.authenticated(
                userDetails,
                "N/A",
                userDetails.getAuthorities()
        );

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder();
        JwtEncodingContext context = JwtEncodingContext.with(
                        JwsHeader.with(SignatureAlgorithm.RS512),
                        claims
                )
                .registeredClient(registeredClient)
                .principal(principal)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                .build();

        jwtTokenCustomizer.customize(context);

        assertThat(context.getClaims().build().getClaimAsStringList("roles"))
                .isEqualTo(List.of("ROLE_ADMIN"));
    }
}
