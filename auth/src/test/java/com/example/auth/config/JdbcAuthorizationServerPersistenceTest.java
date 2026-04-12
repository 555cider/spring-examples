package com.example.auth.config;

import com.example.auth.service.JdbcUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
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

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    void startupCreatesNormalizedAuthorityTableAndSeedsDefaultRoles() throws Exception {
        ApplicationRunner seedUserRunner = context.getBean("seedUsers", ApplicationRunner.class);

        jdbcTemplate.update("""
                delete from my_schema.user_authorities
                where user_id in (
                    select id
                    from my_schema.users
                    where username in (?, ?)
                )
                """, "user", "admin");
        jdbcTemplate.update("""
                delete from my_schema.users
                where username in (?, ?)
                """, "user", "admin");

        seedUserRunner.run(new DefaultApplicationArguments(new String[0]));

        Integer authorityTableCount = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.tables
                where table_schema = 'my_schema'
                  and table_name = 'user_authorities'
                """, Integer.class);

        assertThat(authorityTableCount).isEqualTo(1);

        Integer roleCount = jdbcTemplate.queryForObject("""
                select count(*)
                from my_schema.user_authorities ua
                join my_schema.users u on u.id = ua.user_id
                where u.username = ?
                  and ua.authority = ?
                """, Integer.class, "user", "ROLE_USER");

        assertThat(roleCount).isEqualTo(1);

        Integer adminRoleCount = jdbcTemplate.queryForObject("""
                select count(*)
                from my_schema.user_authorities ua
                join my_schema.users u on u.id = ua.user_id
                where u.username = ?
                  and ua.authority = ?
                """, Integer.class, "admin", "ROLE_ADMIN");

        assertThat(adminRoleCount).isEqualTo(1);

        assertThat(context.getBean("userDetailsService", JdbcUserDetailsService.class)
                .loadUserByUsername("admin")
                .getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void userDetailsServiceLoadsAuthoritiesFromNormalizedTable() {
        JdbcUserDetailsService userDetailsService = context.getBean("userDetailsService", JdbcUserDetailsService.class);

        jdbcTemplate.execute("""
                create table if not exists my_schema.user_authorities (
                    user_id bigint not null,
                    authority varchar(100) not null,
                    primary key (user_id, authority)
                )
                """);

        Long userId = jdbcTemplate.queryForObject(
                "select id from my_schema.users where username = ?",
                Long.class,
                "user"
        );

        jdbcTemplate.update("""
                insert into my_schema.user_authorities (user_id, authority)
                values (?, ?)
                on conflict do nothing
                """,
                userId,
                "ROLE_ADMIN"
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername("user");

        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_ADMIN");
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
