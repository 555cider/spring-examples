package com.example.auth.config;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import javax.sql.DataSource;

import com.example.auth.domain.User;
import com.example.auth.repository.UserAuthorityRepository;
import com.example.auth.repository.UserRepository;
import com.example.auth.service.OidcClaimService;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

@Configuration
@EnableConfigurationProperties(AuthApplicationProperties.class)
public class AuthorizationServerConfig {
    private static final Set<String> LOCAL_REDIRECT_URIS = Set.of(
            "http://127.0.0.1:8011/login/oauth2/code/my-registration",
            "http://127.0.0.1:8011/oauth2/code/my-registration",
            "http://localhost:8011/login/oauth2/code/my-registration",
            "http://localhost:8011/oauth2/code/my-registration"
    );


    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcRegisteredClientRepository(jdbcTemplate);
    }

    @Bean
    public OAuth2AuthorizationService authorizationService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository
    ) {
        return new org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService(
                jdbcTemplate,
                registeredClientRepository
        );
    }

    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository
    ) {
        return new org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService(
                jdbcTemplate,
                registeredClientRepository
        );
    }

    @Bean
    @ConditionalOnProperty(name = "app.auth.demo-client.enabled", havingValue = "true")
    public ApplicationRunner seedRegisteredClient(
            RegisteredClientRepository registeredClientRepository,
            PasswordEncoder passwordEncoder,
            AuthApplicationProperties properties
    ) {
        return args -> {
            String clientId = properties.getAuth().getDemoClient().getClientId();
            if (registeredClientRepository.findByClientId(clientId) != null) {
                return;
            }

            RegisteredClient.Builder client = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId(clientId)
                    .clientSecret(passwordEncoder.encode(properties.getAuth().getDemoClient().getClientSecret()))
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                    .scope(OidcScopes.OPENID)
                    .scope("profile")
                    .scope("email");

            supportedRedirectUris(properties.getClient().getRedirectUri()).forEach(client::redirectUri);

            registeredClientRepository.save(client.build());
        };
    }

    @Bean
    @ConditionalOnProperty(name = "app.auth.demo-users.enabled", havingValue = "true")
    public ApplicationRunner seedUsers(
            UserRepository userRepository,
            UserAuthorityRepository userAuthorityRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            ensureUser(userRepository, userAuthorityRepository, passwordEncoder,
                    "user", "user@example.com", "tenant-alpha", List.of("ROLE_USER"));
            ensureUser(userRepository, userAuthorityRepository, passwordEncoder,
                    "teammate", "teammate@example.com", "tenant-alpha", List.of("ROLE_USER"));
            ensureUser(userRepository, userAuthorityRepository, passwordEncoder,
                    "outsider", "outsider@example.com", "tenant-bravo", List.of("ROLE_USER"));
            ensureUser(userRepository, userAuthorityRepository, passwordEncoder,
                    "admin", "admin@example.com", "tenant-alpha", List.of("ROLE_ADMIN"));
        };
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return NimbusJwtDecoder.withJwkSource(jwkSource).build();
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings(AuthApplicationProperties properties) {
        return AuthorizationServerSettings.builder()
                .issuer(properties.getAuth().getIssuer())
                .authorizationEndpoint("/oauth2/authorization")
                .jwkSetEndpoint("/.well-known/jwks.json")
                .build();
    }

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer(OidcClaimService oidcClaimService) {
        return context -> {
            Authentication principal = context.getPrincipal();
            if (principal == null) {
                return;
            }

            if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
                context.getClaims().claim("roles", oidcClaimService.roleClaims(principal));
                context.getClaims().claim("tenant", oidcClaimService.tenantClaim(principal));
                return;
            }

            if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) {
                oidcClaimService.idTokenClaims(principal)
                        .forEach((claimName, claimValue) -> context.getClaims().claim(claimName, claimValue));
            }
        };
    }

    private Set<String> supportedRedirectUris(String configuredRedirectUri) {
        Set<String> redirectUris = new LinkedHashSet<>();
        redirectUris.add(configuredRedirectUri);

        if (LOCAL_REDIRECT_URIS.contains(configuredRedirectUri)) {
            redirectUris.addAll(LOCAL_REDIRECT_URIS);
        }

        return redirectUris;
    }

    private void ensureUser(
            UserRepository userRepository,
            UserAuthorityRepository userAuthorityRepository,
            PasswordEncoder passwordEncoder,
            String username,
            String email,
            String tenantId,
            List<String> authorities
    ) {
        userRepository.findByUsername(username).ifPresentOrElse(existingUser -> {
                    userAuthorityRepository.addAuthorities(existingUser.getId(), authorities);
                },
                () -> {
                    User savedUser = userRepository.save(new User(
                            null,
                            username,
                            passwordEncoder.encode("1234"),
                            email,
                            tenantId,
                            null,
                            null
                    ));

                    userAuthorityRepository.addAuthorities(savedUser.getId(), authorities);
                });
    }
}
