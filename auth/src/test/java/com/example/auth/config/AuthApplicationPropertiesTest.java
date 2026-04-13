package com.example.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class AuthApplicationPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void bindsDemoClientSettingsWhenConfigured() {
        contextRunner.withPropertyValues(
                        "app.auth.issuer=http://localhost:8080/auth",
                        "app.auth.demo-client.enabled=true",
                        "app.auth.demo-client.client-id=client_id_1",
                        "app.auth.demo-client.client-secret=client_secret_1",
                        "app.client.redirect-uri=http://127.0.0.1:8011/login/oauth2/code/my-registration"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    AuthApplicationProperties properties = context.getBean(AuthApplicationProperties.class);

                    assertThat(properties.getAuth().getIssuer()).isEqualTo("http://localhost:8080/auth");
                    assertThat(properties.getAuth().getDemoClient().isEnabled()).isTrue();
                    assertThat(properties.getAuth().getDemoClient().getClientId()).isEqualTo("client_id_1");
                    assertThat(properties.getAuth().getDemoClient().getClientSecret()).isEqualTo("client_secret_1");
                    assertThat(properties.getClient().getRedirectUri())
                            .isEqualTo("http://127.0.0.1:8011/login/oauth2/code/my-registration");
                });
    }

    @Test
    void failsFastWhenDemoClientIsEnabledWithoutClientSecret() {
        contextRunner.withPropertyValues(
                        "app.auth.issuer=http://localhost:8080/auth",
                        "app.auth.demo-client.enabled=true",
                        "app.auth.demo-client.client-id=client_id_1",
                        "app.client.redirect-uri=http://127.0.0.1:8011/login/oauth2/code/my-registration"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining("app.auth.demo-client.client-secret");
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AuthApplicationProperties.class)
    static class TestConfig {
    }
}
