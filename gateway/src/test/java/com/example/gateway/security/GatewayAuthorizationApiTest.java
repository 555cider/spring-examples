package com.example.gateway.security;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.profiles.active=local",
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.cloud.nacos.discovery.register-enabled=false"
        })
class GatewayAuthorizationApiTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ReactiveJwtDecoder jwtDecoder;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://127.0.0.1:" + port)
                .build();

        Mockito.reset(jwtDecoder);
    }

    @Test
    void meReturnsUsernameAndAuthoritiesFromJwtRolesClaim() {
        when(jwtDecoder.decode(eq("user-token"))).thenReturn(Mono.just(jwt("user-token", "alice", List.of("ROLE_USER"), null)));

        webTestClient.get()
                .uri("/api/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer user-token")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .value(response -> {
                    assertThat(response.get("username")).isEqualTo("alice");
                    assertAuthorities(response, "ROLE_USER");
                });
    }

    @Test
    void meRequiresAuthentication() {
        webTestClient.get()
                .uri("/api/me")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void permittedPublicPathIsNotBlockedBySecurity() {
        webTestClient.get()
                .uri("/public/ping")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void mePreservesScopeAuthoritiesAlongsideRoles() {
        when(jwtDecoder.decode(eq("scoped-user-token"))).thenReturn(Mono.just(
                jwt("scoped-user-token", "alice", List.of("ROLE_USER"), "profile reports:read")));

        webTestClient.get()
                .uri("/api/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer scoped-user-token")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .value(response -> {
                    assertThat(response.get("username")).isEqualTo("alice");
                    assertAuthorities(response, "ROLE_USER", "SCOPE_profile", "SCOPE_reports:read");
                });
    }

    @Test
    void adminReportsRejectsRoleUser() {
        when(jwtDecoder.decode(eq("user-token"))).thenReturn(Mono.just(jwt("user-token", "alice", List.of("ROLE_USER"), null)));

        webTestClient.get()
                .uri("/api/admin/reports")
                .header(HttpHeaders.AUTHORIZATION, "Bearer user-token")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void adminReportsRequiresAuthentication() {
        webTestClient.get()
                .uri("/api/admin/reports")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void adminReportsAllowsRoleAdmin() {
        when(jwtDecoder.decode(eq("admin-token"))).thenReturn(Mono.just(jwt("admin-token", "admin", List.of("ROLE_ADMIN"), null)));

        webTestClient.get()
                .uri("/api/admin/reports")
                .header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.report").isEqualTo("monthly-summary")
                .jsonPath("$.requestedBy").isEqualTo("admin");
    }

    @Test
    void documentOwnerCanReadOwnedDocument() {
        when(jwtDecoder.decode(eq("user-token"))).thenReturn(Mono.just(jwt("user-token", "user", List.of("ROLE_USER"), null)));

        webTestClient.get()
                .uri("/api/documents/doc-user")
                .header(HttpHeaders.AUTHORIZATION, "Bearer user-token")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("doc-user")
                .jsonPath("$.title").isEqualTo("User Document")
                .jsonPath("$.ownerUsername").isEqualTo("user");
    }

    @Test
    void roleUserCannotReadAnotherUsersDocument() {
        when(jwtDecoder.decode(eq("user-token"))).thenReturn(Mono.just(jwt("user-token", "user", List.of("ROLE_USER"), null)));

        webTestClient.get()
                .uri("/api/documents/doc-admin")
                .header(HttpHeaders.AUTHORIZATION, "Bearer user-token")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void documentRequiresAuthentication() {
        webTestClient.get()
                .uri("/api/documents/doc-user")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void adminCanReadAnotherUsersDocument() {
        when(jwtDecoder.decode(eq("admin-token"))).thenReturn(Mono.just(jwt("admin-token", "admin", List.of("ROLE_ADMIN"), null)));

        webTestClient.get()
                .uri("/api/documents/doc-user")
                .header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("doc-user")
                .jsonPath("$.title").isEqualTo("User Document")
                .jsonPath("$.ownerUsername").isEqualTo("user");
    }

    @Test
    void missingDocumentReturnsNotFound() {
        when(jwtDecoder.decode(eq("admin-token"))).thenReturn(Mono.just(jwt("admin-token", "admin", List.of("ROLE_ADMIN"), null)));

        webTestClient.get()
                .uri("/api/documents/missing")
                .header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
                .exchange()
                .expectStatus().isNotFound();
    }

    private void assertAuthorities(Map<String, Object> response, String... expectedAuthorities) {
        assertThat(response).containsKey("authorities");
        assertThat(response.get("authorities")).isInstanceOf(List.class);

        @SuppressWarnings("unchecked")
        List<String> authorities = (List<String>) response.get("authorities");

        assertThat(authorities).isSorted();
        assertThat(authorities).contains(expectedAuthorities);
    }

    private Jwt jwt(String tokenValue, String subject, List<String> roles, String scope) {
        Instant issuedAt = Instant.parse("2024-01-01T00:00:00Z");
        Jwt.Builder builder = Jwt.withTokenValue(tokenValue)
                .header("alg", "RS512")
                .claim("sub", subject)
                .claim("roles", roles)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(300));

        if (scope != null) {
            builder.claim("scope", scope);
        }

        return builder.build();
    }

    @TestConfiguration
    static class JwtDecoderConfiguration {

        @Bean
        @Primary
        ReactiveJwtDecoder jwtDecoder() {
            return Mockito.mock(ReactiveJwtDecoder.class);
        }
    }
}
