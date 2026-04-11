package com.example.auth.config;

import com.example.auth.service.JdbcUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.ApplicationContext;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;

import static org.assertj.core.api.Assertions.assertThat;

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
}
