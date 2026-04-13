package com.example.auth.config;

import java.io.IOException;
import java.net.URI;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;
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

    private static final String REDIRECT_URI = "http://127.0.0.1:8011/login/oauth2/code/my-registration";
    private static final String CODE_VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
    private static final String CODE_CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";
    private static final Pattern CSRF_PATTERN = Pattern.compile(
            "name=\"_csrf\"[^>]*value=\"([^\"]+)\"|value=\"([^\"]+)\"[^>]*name=\"_csrf\""
    );

    private HttpClient httpClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${local.server.port}")
    private int port;

    @org.springframework.beans.factory.annotation.Autowired
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        this.httpClient = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .followRedirects(Redirect.NEVER)
                .build();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://127.0.0.1:8011/oauth2/code/my-registration",
            "http://127.0.0.1:8011/login/oauth2/code/my-registration",
            "http://localhost:8011/oauth2/code/my-registration",
            "http://localhost:8011/login/oauth2/code/my-registration"
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

    @org.junit.jupiter.api.Test
    void authorizationEndpointUsesForwardedHeadersForLoginRedirect() throws Exception {
        URI uri = UriComponentsBuilder.fromUriString("http://127.0.0.1:" + port + "/oauth2/authorization")
                .queryParam("response_type", "code")
                .queryParam("client_id", "client_id_1")
                .queryParam("scope", "openid")
                .queryParam("redirect_uri", "http://localhost:8011/login/oauth2/code/my-registration")
                .queryParam("state", "state-123")
                .queryParam("nonce", "nonce-123")
                .queryParam("code_challenge", "Z_P4EKbGwIkA01e3Y5fp4tMCvn_Ae5nUw7qY7XwkTrQ")
                .queryParam("code_challenge_method", "S256")
                .build()
                .encode()
                .toUri();

        HttpResponse<Void> response = httpClient.send(
                HttpRequest.newBuilder(uri)
                        .header("X-Forwarded-Proto", "http")
                        .header("X-Forwarded-Host", "localhost")
                        .header("X-Forwarded-Port", "8080")
                        .header("X-Forwarded-Prefix", "/auth")
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.discarding()
        );

        assertEquals(302, response.statusCode());
        assertEquals("http://localhost:8080/auth/login", response.headers().firstValue("Location").orElseThrow());
    }

    @Test
    void authorizationCodeFlowReturnsOidcClaimsInTokensAndUserInfo() throws Exception {
        TokenResponse tokenResponse = authorizeAndExchangeTokens("openid profile email");

        Jwt accessToken = jwtDecoder.decode(tokenResponse.accessToken());
        Jwt idToken = jwtDecoder.decode(tokenResponse.idToken());

        assertThat(accessToken.getClaimAsStringList("roles")).containsExactly("ROLE_ADMIN");
        assertThat(accessToken.getClaimAsString("tenant")).isEqualTo("tenant-alpha");
        assertThat(idToken.getClaimAsString("preferred_username")).isEqualTo("admin");
        assertThat(idToken.getClaimAsString("email")).isEqualTo("admin@example.com");
        assertThat(idToken.getClaimAsString("tenant")).isEqualTo("tenant-alpha");
        assertThat(idToken.getClaimAsStringList("roles")).containsExactly("ROLE_ADMIN");

        HttpResponse<String> userInfoResponse = httpClient.send(
                HttpRequest.newBuilder(baseUri("/userinfo"))
                        .header("Authorization", "Bearer " + tokenResponse.accessToken())
                        .GET()
                        .build(),
                BodyHandlers.ofString()
        );

        assertThat(userInfoResponse.statusCode())
                .withFailMessage("UserInfo endpoint returned %s with body: %s", userInfoResponse.statusCode(), userInfoResponse.body())
                .isEqualTo(200);

        Map<String, Object> userInfo = objectMapper.readValue(
                userInfoResponse.body(),
                new TypeReference<>() {
                }
        );

        assertThat(userInfo)
                .containsEntry("sub", "admin")
                .containsEntry("preferred_username", "admin")
                .containsEntry("email", "admin@example.com")
                .containsEntry("tenant", "tenant-alpha")
                .containsEntry("email_verified", true);
        assertThat(userInfo.get("roles")).isEqualTo(java.util.List.of("ROLE_ADMIN"));
    }

    @Test
    void userInfoHonorsRequestedScopesDuringAuthorizationCodeFlow() throws Exception {
        TokenResponse tokenResponse = authorizeAndExchangeTokens("openid");

        HttpResponse<String> userInfoResponse = httpClient.send(
                HttpRequest.newBuilder(baseUri("/userinfo"))
                        .header("Authorization", "Bearer " + tokenResponse.accessToken())
                        .GET()
                        .build(),
                BodyHandlers.ofString()
        );

        assertThat(userInfoResponse.statusCode())
                .withFailMessage("UserInfo endpoint returned %s with body: %s", userInfoResponse.statusCode(), userInfoResponse.body())
                .isEqualTo(200);

        Map<String, Object> userInfo = objectMapper.readValue(
                userInfoResponse.body(),
                new TypeReference<>() {
                }
        );

        assertThat(userInfo)
                .containsEntry("sub", "admin")
                .containsEntry("tenant", "tenant-alpha");
        assertThat(userInfo.get("roles")).isEqualTo(java.util.List.of("ROLE_ADMIN"));
        assertThat(userInfo).doesNotContainKeys("email", "email_verified", "preferred_username", "updated_at");
    }

    private TokenResponse authorizeAndExchangeTokens(String scope) throws Exception {
        HttpResponse<Void> authorizeResponse = httpClient.send(
                HttpRequest.newBuilder(authorizationUri(scope)).GET().build(),
                BodyHandlers.discarding()
        );

        assertThat(authorizeResponse.statusCode()).isEqualTo(302);
        assertThat(authorizeResponse.headers().firstValue("Location").orElse("")).contains("/login");

        HttpResponse<String> loginPage = httpClient.send(
                HttpRequest.newBuilder(baseUri("/login")).GET().build(),
                BodyHandlers.ofString()
        );

        String csrfToken = extractCsrfToken(loginPage.body());

        HttpResponse<Void> loginResponse = httpClient.send(
                HttpRequest.newBuilder(baseUri("/login"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(BodyPublishers.ofString(formBody(Map.of(
                                "username", "admin",
                                "password", "1234",
                                "_csrf", csrfToken
                        ))))
                        .build(),
                BodyHandlers.discarding()
        );

        assertThat(loginResponse.statusCode()).isEqualTo(302);

        URI callbackUri = followAuthorizationRedirects(loginResponse.headers().firstValue("Location").orElseThrow());
        String code = UriComponentsBuilder.fromUri(callbackUri).build().getQueryParams().getFirst("code");

        assertThat(code).isNotBlank();

        HttpResponse<String> tokenResponse = httpClient.send(
                HttpRequest.newBuilder(baseUri("/oauth2/token"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(BodyPublishers.ofString(formBody(Map.of(
                                "grant_type", "authorization_code",
                                "client_id", "client_id_1",
                                "client_secret", "client_secret_1",
                                "redirect_uri", REDIRECT_URI,
                                "code", code,
                                "code_verifier", CODE_VERIFIER
                        ))))
                        .build(),
                BodyHandlers.ofString()
        );

        assertThat(tokenResponse.statusCode())
                .withFailMessage("Token endpoint returned %s with body: %s", tokenResponse.statusCode(), tokenResponse.body())
                .isEqualTo(200);

        Map<String, Object> tokenPayload = objectMapper.readValue(
                tokenResponse.body(),
                new TypeReference<>() {
                }
        );

        return new TokenResponse(
                tokenPayload.get("access_token").toString(),
                tokenPayload.get("id_token").toString()
        );
    }

    private URI authorizationUri(String scope) {
        return UriComponentsBuilder.fromUriString("http://127.0.0.1:" + port + "/oauth2/authorization")
                .queryParam("response_type", "code")
                .queryParam("client_id", "client_id_1")
                .queryParam("scope", scope)
                .queryParam("redirect_uri", REDIRECT_URI)
                .queryParam("state", "state-123")
                .queryParam("nonce", "nonce-123")
                .queryParam("code_challenge", CODE_CHALLENGE)
                .queryParam("code_challenge_method", "S256")
                .build()
                .encode()
                .toUri();
    }

    private URI followAuthorizationRedirects(String location) throws Exception {
        URI current = resolve(location);

        for (int redirectCount = 0; redirectCount < 5; redirectCount++) {
            if (REDIRECT_URI.startsWith(current.getScheme() + "://" + current.getHost())
                    && current.getQuery() != null
                    && current.getQuery().contains("code=")) {
                return current;
            }

            HttpResponse<Void> response = httpClient.send(
                    HttpRequest.newBuilder(current).GET().build(),
                    BodyHandlers.discarding()
            );

            assertThat(response.statusCode()).isEqualTo(302);
            current = resolve(response.headers().firstValue("Location").orElseThrow());
        }

        throw new IllegalStateException("Authorization flow did not yield a callback code");
    }

    private URI resolve(String location) {
        return baseUri("/").resolve(location);
    }

    private URI baseUri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    private String extractCsrfToken(String html) {
        Matcher matcher = CSRF_PATTERN.matcher(html);
        if (!matcher.find()) {
            throw new IllegalStateException("CSRF token not found in login page");
        }

        return matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
    }

    private String formBody(Map<String, String> fields) {
        Map<String, String> orderedFields = new LinkedHashMap<>(fields);
        return orderedFields.entrySet().stream()
                .map(entry -> urlEncode(entry.getKey()) + "=" + urlEncode(entry.getValue()))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record TokenResponse(String accessToken, String idToken) {
    }
}
