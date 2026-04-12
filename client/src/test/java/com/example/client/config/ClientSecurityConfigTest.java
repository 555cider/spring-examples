package com.example.client.config;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = "spring.main.web-application-type=servlet")
@ActiveProfiles("local")
class ClientSecurityConfigTest {

    private static HttpServer issuerServer;

    private static String issuerUri;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @AfterAll
    static void stopIssuerServer() {
        if (issuerServer != null) {
            issuerServer.stop(0);
        }
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        ensureIssuerServer();
        registry.add("spring.security.oauth2.client.provider.my-provider.issuer-uri", () -> issuerUri);
    }

    private static void ensureIssuerServer() {
        if (issuerServer != null) {
            return;
        }

        try {
            issuerServer = HttpServer.create(new InetSocketAddress(0), 0);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }

        issuerUri = "http://127.0.0.1:" + issuerServer.getAddress().getPort() + "/auth";
        issuerServer.createContext("/auth/.well-known/openid-configuration", ClientSecurityConfigTest::handleDiscoveryRequest);
        issuerServer.start();
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void anonymousProfileRequestRedirectsToOAuth2AuthorizationEndpoint() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/oauth2/authorization/my-registration"));
    }

    @Test
    void publicIndexRequestReturnsOkAndRendersIndexView() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    void authenticatedProfileRequestReturnsOkAndRendersProfileView() throws Exception {
        mockMvc.perform(get("/profile").with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"));
    }

    @Test
    void applicationRegistersDiscoveryClient() throws Exception {
        Class<?> discoveryClientType = Class.forName("org.springframework.cloud.client.discovery.DiscoveryClient");

        assertThat(context.getBeanNamesForType(discoveryClientType))
                .isNotEmpty();
    }

    private static void handleDiscoveryRequest(HttpExchange exchange) throws IOException {
        String body = """
                {
                  "issuer": "%s",
                  "authorization_endpoint": "%s/oauth2/authorization",
                  "token_endpoint": "%s/oauth2/token",
                  "jwks_uri": "%s/.well-known/jwks.json",
                  "userinfo_endpoint": "%s/oauth2/userinfo",
                  "subject_types_supported": ["public"],
                  "id_token_signing_alg_values_supported": ["RS256"]
                }
                """.formatted(issuerUri, issuerUri, issuerUri, issuerUri, issuerUri);
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }
}
