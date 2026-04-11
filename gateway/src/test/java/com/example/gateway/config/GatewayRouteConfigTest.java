package com.example.gateway.config;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.profiles.active=local",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.discovery.register-enabled=false"
})
class GatewayRouteConfigTest {

    @Autowired
    private RouteLocator routeLocator;

    @Test
    void authRouteUsesLoadBalancedAuthService() {
        Route authRoute = routeLocator.getRoutes()
                .filter(route -> route.getId().equals("route_auth"))
                .single()
                .block();

        assertThat(authRoute).isNotNull();
        assertThat(authRoute.getUri()).isEqualTo(URI.create("lb://auth"));
    }
}
