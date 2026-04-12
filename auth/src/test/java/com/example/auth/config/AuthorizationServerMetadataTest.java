package com.example.auth.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.profiles.active=test",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.discovery.register-enabled=false"
})
class AuthorizationServerMetadataTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private Environment environment;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void openIdMetadataIsPublished() throws Exception {
        mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").value("http://localhost:8080/auth"))
                .andExpect(jsonPath("$.authorization_endpoint").value("http://localhost:8080/auth/oauth2/authorization"))
                .andExpect(jsonPath("$.jwks_uri").value("http://localhost:8080/auth/.well-known/jwks.json"));
    }

    @Test
    void jwkSetEndpointIsPublicAndOmitsPrivateKeyMaterial() throws Exception {
        mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].kid").value("auth-server-key"))
                .andExpect(jsonPath("$.keys[0].n").exists())
                .andExpect(jsonPath("$.keys[0].e").exists())
                .andExpect(jsonPath("$.keys[0].d").doesNotExist());
    }

    @Test
    void authUsesDedicatedSessionCookieName() {
        org.assertj.core.api.Assertions.assertThat(environment.getProperty("server.servlet.session.cookie.name"))
                .isEqualTo("AUTHSESSION");
    }
}
