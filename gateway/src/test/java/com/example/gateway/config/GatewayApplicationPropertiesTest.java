package com.example.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayApplicationPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void bindsGatewaySecurityAndRateLimitSettings() {
        contextRunner.withPropertyValues(
                        "app.gateway.auth-server.issuer-uri=http://localhost:8080/auth",
                        "app.gateway.rate-limit.replenish-rate=100",
                        "app.gateway.rate-limit.burst-capacity=100",
                        "app.gateway.rate-limit.requested-tokens=1"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    GatewayApplicationProperties properties = context.getBean(GatewayApplicationProperties.class);

                    assertThat(properties.getAuthServer().getIssuerUri()).isEqualTo("http://localhost:8080/auth");
                    assertThat(properties.getRateLimit().getReplenishRate()).isEqualTo(100);
                    assertThat(properties.getRateLimit().getBurstCapacity()).isEqualTo(100);
                    assertThat(properties.getRateLimit().getRequestedTokens()).isEqualTo(1);
                });
    }

    @Test
    void failsFastWhenGatewayIssuerIsMissing() {
        contextRunner.withPropertyValues(
                        "app.gateway.rate-limit.replenish-rate=100",
                        "app.gateway.rate-limit.burst-capacity=100",
                        "app.gateway.rate-limit.requested-tokens=1"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasMessageContaining("app.gateway.auth-server.issuer-uri");
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(GatewayApplicationProperties.class)
    static class TestConfig {
    }
}
