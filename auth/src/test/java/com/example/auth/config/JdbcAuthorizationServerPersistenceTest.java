package com.example.auth.config;

import com.example.auth.service.JdbcUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.ApplicationContext;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;

import java.time.Instant;
import java.security.Principal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest(properties = {
        "spring.profiles.active=test",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.discovery.register-enabled=false"
})
@ActiveProfiles("test")
class JdbcAuthorizationServerPersistenceTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void authorizationServerPersistenceBeansAreJdbcBacked() {
        RegisteredClientRepository registeredClientRepository = context.getBean(RegisteredClientRepository.class);
        OAuth2AuthorizationService authorizationService = context.getBean(OAuth2AuthorizationService.class);
        OAuth2AuthorizationConsentService consentService = context.getBean(OAuth2AuthorizationConsentService.class);
        JdbcUserDetailsService userDetailsService = context.getBean("userDetailsService", JdbcUserDetailsService.class);

        assertThat(registeredClientRepository).isInstanceOf(JdbcRegisteredClientRepository.class);
        assertThat(registeredClientRepository.findByClientId("client_id_1")).isNotNull();
        assertThat(authorizationService).isInstanceOf(JdbcOAuth2AuthorizationService.class);
        assertThat(consentService).isInstanceOf(JdbcOAuth2AuthorizationConsentService.class);
        assertThat(userDetailsService).isInstanceOf(JdbcUserDetailsService.class);
        assertThat(userDetailsService.loadUserByUsername("user").getUsername()).isEqualTo("user");
    }

    @Test
    void authorizationServicePersistsAuthorizationCodeRecords() {
        RegisteredClientRepository registeredClientRepository = context.getBean(RegisteredClientRepository.class);
        OAuth2AuthorizationService authorizationService = context.getBean(OAuth2AuthorizationService.class);
        RegisteredClient registeredClient = registeredClientRepository.findByClientId("client_id_1");

        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(registeredClient)
                .id("authorization-id")
                .principalName("user")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizedScopes(Set.of("openid"))
                .attribute(OAuth2ParameterNames.STATE, "state-123")
                .token(new OAuth2AuthorizationCode(
                        "code-value",
                        Instant.now(),
                        Instant.now().plusSeconds(300)
                ))
                .build();

        authorizationService.save(authorization);

        assertThat(authorizationService.findById("authorization-id")).isNotNull();
    }

    @Test
    void authorizationServiceCanReadBackAuthorizationWithAuthenticatedPrincipal() {
        RegisteredClientRepository registeredClientRepository = context.getBean(RegisteredClientRepository.class);
        OAuth2AuthorizationService authorizationService = context.getBean(OAuth2AuthorizationService.class);
        JdbcUserDetailsService userDetailsService = context.getBean("userDetailsService", JdbcUserDetailsService.class);
        RegisteredClient registeredClient = registeredClientRepository.findByClientId("client_id_1");
        UserDetails userDetails = userDetailsService.loadUserByUsername("user");

        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(registeredClient)
                .id("authorization-with-principal")
                .principalName(userDetails.getUsername())
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizedScopes(Set.of("openid"))
                .attribute(Principal.class.getName(), UsernamePasswordAuthenticationToken.authenticated(
                        userDetails,
                        "N/A",
                        userDetails.getAuthorities()
                ))
                .token(new OAuth2AuthorizationCode(
                        "code-value-with-principal",
                        Instant.now(),
                        Instant.now().plusSeconds(300)
                ))
                .build();

        authorizationService.save(authorization);

        assertThatCode(() -> authorizationService.findByToken(
                "code-value-with-principal",
                new OAuth2TokenType(OAuth2ParameterNames.CODE)
        )).doesNotThrowAnyException();
    }
}
