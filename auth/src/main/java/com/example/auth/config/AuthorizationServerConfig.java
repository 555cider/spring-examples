package com.example.auth.config;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;

@Configuration
public class AuthorizationServerConfig {

    @Bean
    public RegisteredClientRepository registeredClientRepository(
            PasswordEncoder passwordEncoder,
            @Value("${app.client.redirect-uri}") String redirectUri
    ) {
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

        return new InMemoryRegisteredClientRepository(client.build());
    }

    @Bean
    public OAuth2AuthorizationService authorizationService() {
        return new org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService();
    }

    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService() {
        return new org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationConsentService();
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

        if ("http://127.0.0.1:8011/login/oauth2/code/my-registration".equals(configuredRedirectUri)) {
            redirectUris.add("http://127.0.0.1:8011/oauth2/code/my-registration");
        } else if ("http://127.0.0.1:8011/oauth2/code/my-registration".equals(configuredRedirectUri)) {
            redirectUris.add("http://127.0.0.1:8011/login/oauth2/code/my-registration");
        }

        return redirectUris;
    }
}
