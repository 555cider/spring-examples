package com.example.auth.config;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import javax.sql.DataSource;

import com.example.auth.domain.User;
import com.example.auth.repository.UserAuthorityRepository;
import com.example.auth.repository.UserRepository;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;

@Configuration
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
    public ApplicationRunner seedRegisteredClient(
            RegisteredClientRepository registeredClientRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.client.redirect-uri}") String redirectUri
    ) {
        return args -> {
            if (registeredClientRepository.findByClientId("client_id_1") != null) {
                return;
            }

            RegisteredClient.Builder client = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId("client_id_1")
                    .clientSecret(passwordEncoder.encode("client_secret_1"))
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                    .scope(OidcScopes.OPENID)
                    .scope("profile")
                    .scope("email");

            supportedRedirectUris(redirectUri).forEach(client::redirectUri);

            registeredClientRepository.save(client.build());
        };
    }

    @Bean
    public ApplicationRunner seedUsers(
            UserRepository userRepository,
            UserAuthorityRepository userAuthorityRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            ensureUser(userRepository, userAuthorityRepository, passwordEncoder,
                    "user", "user@example.com", List.of("ROLE_USER"));
            ensureUser(userRepository, userAuthorityRepository, passwordEncoder,
                    "admin", "admin@example.com", List.of("ROLE_ADMIN"));
        };
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return NimbusJwtDecoder.withJwkSource(jwkSource).build();
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings(@Value("${app.auth.issuer}") String issuer) {
        return AuthorizationServerSettings.builder()
                .issuer(issuer)
                .authorizationEndpoint("/oauth2/authorization")
                .jwkSetEndpoint("/.well-known/jwks.json")
                .build();
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
            List<String> authorities
    ) {
        userRepository.findByUsername(username).ifPresentOrElse(existingUser -> {
                    if (!userAuthorityRepository.findAuthoritiesByUserId(existingUser.getId()).containsAll(authorities)) {
                        userAuthorityRepository.replaceAuthorities(existingUser.getId(), authorities);
                    }
                },
                () -> {
                    User savedUser = userRepository.save(new User(
                            null,
                            username,
                            passwordEncoder.encode("1234"),
                            email,
                            null,
                            null
                    ));

                    userAuthorityRepository.replaceAuthorities(savedUser.getId(), authorities);
                });
    }
}
