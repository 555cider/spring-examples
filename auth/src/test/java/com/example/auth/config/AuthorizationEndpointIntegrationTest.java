package com.example.auth.config;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.util.UriComponentsBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.profiles.active=test",
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.cloud.nacos.discovery.register-enabled=false"
        }
)
class AuthorizationEndpointIntegrationTest {

    private HttpClient httpClient;

    @Value("${local.server.port}")
    private int port;

    @BeforeEach
    void setUp() {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(Redirect.NEVER)
                .build();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://127.0.0.1:8011/oauth2/code/my-registration",
            "http://127.0.0.1:8011/login/oauth2/code/my-registration"
    })
    void authorizationEndpointAcceptsSupportedRedirectUris(String redirectUri) throws Exception {
        URI uri = UriComponentsBuilder.fromUriString("http://127.0.0.1:" + port + "/oauth2/authorization")
                .queryParam("response_type", "code")
                .queryParam("client_id", "client_id_1")
                .queryParam("scope", "openid")
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", "state-123")
                .queryParam("nonce", "nonce-123")
                .queryParam("code_challenge", "Z_P4EKbGwIkA01e3Y5fp4tMCvn_Ae5nUw7qY7XwkTrQ")
                .queryParam("code_challenge_method", "S256")
                .build()
                .encode()
                .toUri();

        HttpResponse<Void> response = httpClient.send(
                HttpRequest.newBuilder(uri).GET().build(),
                HttpResponse.BodyHandlers.discarding()
        );

        assertEquals(302, response.statusCode());
        assertTrue(response.headers().firstValue("Location").orElse("").contains("/login"));
    }
}
