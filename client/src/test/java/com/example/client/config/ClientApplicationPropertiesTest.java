package com.example.client.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class ClientApplicationPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void bindsOidcClientSettings() {
        contextRunner.withPropertyValues(
                        "app.client.oidc.issuer-uri=http://localhost:8080/auth",
                        "app.client.oidc.client-id=client_id_1",
                        "app.client.oidc.client-secret=client_secret_1"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    ClientApplicationProperties properties = context.getBean(ClientApplicationProperties.class);

                    assertThat(properties.getOidc().getIssuerUri()).isEqualTo("http://localhost:8080/auth");
                    assertThat(properties.getOidc().getClientId()).isEqualTo("client_id_1");
                    assertThat(properties.getOidc().getClientSecret()).isEqualTo("client_secret_1");
                });
    }

    @Test
    void failsFastWhenOidcClientSecretIsMissing() {
        contextRunner.withPropertyValues(
                        "app.client.oidc.issuer-uri=http://localhost:8080/auth",
                        "app.client.oidc.client-id=client_id_1"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasMessageContaining("app.client.oidc.client-secret");
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ClientApplicationProperties.class)
    static class TestConfig {
    }
}
