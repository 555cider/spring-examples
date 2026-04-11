# Spring Boot 4 + Nacos Modernization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade this repository onto a Spring Boot 4.0.0 / Spring Cloud 2025.1.0 / Spring Cloud Alibaba 2025.1.0.0 baseline, replace Eureka with Nacos Discovery, rebuild `auth` as a servlet authorization server, and remove the reactive/blocking hybrid paths.

**Architecture:** Keep `gateway` reactive, move `auth` and `client` to servlet applications, and externalize service registration into Nacos Discovery. Persist authorization server state in PostgreSQL through Spring Security JDBC repositories, keep Redis optional for gateway rate limiting only, and leave distributed configuration out of scope.

**Tech Stack:** Java 21, Gradle 8.14.3 wrapper, Spring Boot 4.0.0, Spring Security 7 authorization server, Spring Cloud Gateway Server WebFlux, Spring Cloud Alibaba Nacos Discovery 2025.1.0.0, PostgreSQL, Redis, Docker Compose for local convenience.

---

## Planned File Map

**Create**
- `build.gradle.kts`
- `gradlew`
- `gradlew.bat`
- `gradle/wrapper/gradle-wrapper.properties`
- `gradle/wrapper/gradle-wrapper.jar`
- `gateway/src/test/java/com/example/gateway/config/GatewayRouteConfigTest.java`
- `auth/src/main/java/com/example/auth/config/AuthorizationServerConfig.java`
- `auth/src/main/java/com/example/auth/config/PemKeyConfig.java`
- `auth/src/test/java/com/example/auth/config/AuthorizationServerMetadataTest.java`
- `auth/src/test/java/com/example/auth/config/JdbcAuthorizationServerPersistenceTest.java`
- `auth/src/test/resources/application-test.yml`
- `auth/src/main/resources/keys/jwt-private_pkcs8.pem`
- `auth/src/main/resources/keys/jwt-public.pem`
- `auth/src/main/resources/sql/oauth2-registered-client.sql`
- `auth/src/main/resources/sql/oauth2-authorization.sql`
- `auth/src/main/resources/sql/oauth2-authorization-consent.sql`
- `auth/src/main/resources/sql/users.sql`
- `client/src/test/java/com/example/client/config/ClientSecurityConfigTest.java`
- `client/src/main/resources/templates/profile.html`
- `README.md`

**Modify**
- `settings.gradle.kts`
- `discovery/build.gradle.kts`
- `gateway/build.gradle.kts`
- `auth/build.gradle.kts`
- `client/build.gradle.kts`
- `gateway/src/main/java/com/example/gateway/GatewayApplication.java`
- `gateway/src/main/java/com/example/gateway/config/SecurityConfig.java`
- `gateway/src/main/resources/application.yml`
- `auth/src/main/java/com/example/auth/AuthApplication.java`
- `auth/src/main/java/com/example/auth/config/SecurityConfig.java`
- `auth/src/main/java/com/example/auth/service/JdbcUserDetailsService.java`
- `auth/src/main/java/com/example/auth/repository/UserRepository.java`
- `auth/src/main/resources/application.yml`
- `client/src/main/java/com/example/client/ClientApplication.java`
- `client/src/main/java/com/example/client/config/SecurityConfig.java`
- `client/src/main/java/com/example/client/controller/ClientController.java`
- `client/src/main/resources/application.yml`
- `client/src/main/resources/templates/index.html`
- `docker-compose.yml`
- `auth/Dockerfile`
- `gateway/Dockerfile`
- `client/Dockerfile`

**Delete**
- `discovery/src/main/java/com/example/discovery/DiscoveryApplication.java`
- `discovery/build.gradle.kts`
- `discovery/src/main/resources/application.yml`
- `discovery/src/main/resources/logback-spring.xml`
- `discovery/Dockerfile`
- `gateway/src/main/java/com/example/gateway/config/TokenConfig.java`
- `gateway/src/main/resources/keys/jwt-private_pkcs8.pem`
- `gateway/src/main/resources/keys/jwt-public.pem`
- `auth/src/main/java/com/example/auth/config/RedisConfig.java`
- `auth/src/main/java/com/example/auth/config/TokenConfig.java`
- `auth/src/main/java/com/example/auth/domain/Client.java`
- `auth/src/main/java/com/example/auth/domain/OAuth2AuthorizationCodeGrantAuthorization.java`
- `auth/src/main/java/com/example/auth/domain/OAuth2AuthorizationGrantAuthorization.java`
- `auth/src/main/java/com/example/auth/domain/OAuth2ClientCredentialsGrantAuthorization.java`
- `auth/src/main/java/com/example/auth/domain/OAuth2UserConsent.java`
- `auth/src/main/java/com/example/auth/domain/OidcAuthorizationCodeGrantAuthorization.java`
- `auth/src/main/java/com/example/auth/domain/mixin/ClaimsHolderMixin.java`
- `auth/src/main/java/com/example/auth/domain/mixin/OAuth2AuthorizationCodeGrantAuthorizationMixin.java`
- `auth/src/main/java/com/example/auth/domain/mixin/OAuth2AuthorizationGrantAuthorizationMixin.java`
- `auth/src/main/java/com/example/auth/domain/mixin/OAuth2AuthorizationResponseTypeMixin.java`
- `auth/src/main/java/com/example/auth/repository/ClientRepository.java`
- `auth/src/main/java/com/example/auth/repository/JdbcRegisteredClientRepository.java`
- `auth/src/main/java/com/example/auth/repository/redis/OAuth2AuthorizationGrantAuthorizationRepository.java`
- `auth/src/main/java/com/example/auth/repository/redis/OAuth2UserConsentRepository.java`
- `auth/src/main/java/com/example/auth/service/RedisOAuth2AuthorizationConsentService.java`
- `auth/src/main/java/com/example/auth/service/RedisOAuth2AuthorizationService.java`
- `auth/src/main/java/com/example/auth/util/ModelMapper.java`
- `auth/src/main/java/com/example/auth/util/Serializer.java`
- `auth/src/main/java/com/example/auth/util/converter/BytesToClaimsHolderConverter.java`
- `auth/src/main/java/com/example/auth/util/converter/BytesToOAuth2AuthorizationRequestConverter.java`
- `auth/src/main/java/com/example/auth/util/converter/BytesToUsernamePasswordAuthenticationTokenConverter.java`
- `auth/src/main/java/com/example/auth/util/converter/ClaimsHolderToBytesConverter.java`
- `auth/src/main/java/com/example/auth/util/converter/OAuth2AuthorizationRequestToBytesConverter.java`
- `auth/src/main/java/com/example/auth/util/converter/UsernamePasswordAuthenticationTokenToBytesConverter.java`
- `client/src/main/java/com/example/client/config/WebClientConfig.java`

### Task 1: Establish the Root Gradle Baseline

**Files:**
- Create: `build.gradle.kts`, `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.properties`, `gradle/wrapper/gradle-wrapper.jar`
- Modify: `settings.gradle.kts`, `discovery/build.gradle.kts`, `gateway/build.gradle.kts`, `auth/build.gradle.kts`, `client/build.gradle.kts`

- [ ] **Step 1: Capture the current build failure**

Run: `./gradlew help`  
Expected: shell failure because the repository does not yet contain a Gradle wrapper.

- [ ] **Step 2: Generate the wrapper with the target Gradle version**

Run:

```bash
docker run --rm -u "$(id -u):$(id -g)" -v "$PWD":/workspace -w /workspace gradle:8.14.3-jdk21 gradle wrapper --gradle-version 8.14.3
```

Expected: `gradlew`, `gradlew.bat`, and the wrapper files appear in the repository root.

- [ ] **Step 3: Write the root build and simplify module build files**

Write `build.gradle.kts`:

```kotlin
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test

plugins {
    id("org.springframework.boot") version "4.0.0" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
    group = "com.example"
    version = "0.0.1"
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")

    repositories {
        mavenCentral()
    }

    extensions.configure(JavaPluginExtension::class.java) {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    extensions.configure(DependencyManagementExtension::class.java) {
        imports {
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.1.0")
            mavenBom("com.alibaba.cloud:spring-cloud-alibaba-dependencies:2025.1.0.0")
        }
    }

    tasks.withType(Test::class.java).configureEach {
        useJUnitPlatform()
    }
}
```

Write `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "spring-projects"

include("discovery")
include("gateway")
include("auth")
include("client")
```

Write `discovery/build.gradle.kts`:

```kotlin
dependencies {
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-server")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
```

Write `gateway/build.gradle.kts`:

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
    implementation("org.springframework.cloud:spring-cloud-starter-gateway")
    implementation("com.github.ulisesbocchio:jasypt-spring-boot-starter:3.0.5")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
```

Write `auth/build.gradle.kts`:

```kotlin
dependencies {
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-authorization-server")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
    implementation("org.postgresql:r2dbc-postgresql")
    implementation("org.modelmapper:modelmapper:3.2.1")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
```

Write `client/build.gradle.kts`:

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
```

- [ ] **Step 4: Verify the wrapper and centralized build work**

Run: `./gradlew help :auth:dependencies --configuration runtimeClasspath`  
Expected: `BUILD SUCCESSFUL` and resolved dependencies for the `auth` module.

- [ ] **Step 5: Commit**

```bash
git add build.gradle.kts settings.gradle.kts gradlew gradlew.bat gradle/wrapper discovery/build.gradle.kts gateway/build.gradle.kts auth/build.gradle.kts client/build.gradle.kts
git commit -m "build: add root gradle baseline for boot 4"
```

### Task 2: Modernize the Gateway Around Nacos Discovery

**Files:**
- Modify: `gateway/build.gradle.kts`, `gateway/src/main/java/com/example/gateway/GatewayApplication.java`, `gateway/src/main/java/com/example/gateway/config/SecurityConfig.java`, `gateway/src/main/resources/application.yml`
- Delete: `gateway/src/main/java/com/example/gateway/config/TokenConfig.java`
- Test: `gateway/src/test/java/com/example/gateway/config/GatewayRouteConfigTest.java`

- [ ] **Step 1: Write the failing gateway route test**

Create `gateway/src/test/java/com/example/gateway/config/GatewayRouteConfigTest.java`:

```java
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
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :gateway:test --tests 'com.example.gateway.config.GatewayRouteConfigTest'`  
Expected: FAIL because the route still points at the old Eureka-era uppercase service name and the module is still on the old gateway starter.

- [ ] **Step 3: Write the minimal gateway implementation**

Write `gateway/build.gradle.kts`:

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
    implementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webflux")
    implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")
    implementation("com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery")
    implementation("com.github.ulisesbocchio:jasypt-spring-boot-starter:3.0.5")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
}
```

Write `gateway/src/main/java/com/example/gateway/GatewayApplication.java`:

```java
package com.example.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableCaching
@EnableDiscoveryClient
@SpringBootApplication(exclude = {org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration.class})
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

}
```

Write `gateway/src/main/java/com/example/gateway/config/SecurityConfig.java`:

```java
package com.example.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@EnableReactiveMethodSecurity
@EnableWebFluxSecurity
@Configuration
public class SecurityConfig {

    private static final String[] PERMITTED_URL = {
            "/",
            "/actuator/**",
            "/resources/**",
            "/public/**",
            "/auth/**"
    };

    @Bean
    public SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(ServerHttpSecurity.CorsSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .authorizeExchange(request -> request
                        .pathMatchers(PERMITTED_URL).permitAll()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(ServerHttpSecurity.OAuth2ResourceServerSpec::jwt)
                .build();
    }
}
```

Write `gateway/src/main/resources/application.yml`:

```yaml
# local
spring:
  config:
    activate:
      on-profile: local
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:9000
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER_ADDR:127.0.0.1:8848}

server:
  port: 8080

---
# dev
spring:
  config:
    activate:
      on-profile: dev
  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT}
      database: 0
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${AUTH_ISSUER_URI}
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER_ADDR}

server:
  port: ${GATEWAY_PORT}

---
# common
spring:
  application:
    name: gateway
  messages:
    basename: messages
  cloud:
    loadbalancer:
      nacos:
        enabled: true
    gateway:
      default-filters:
        - name: LogFilter
      routes:
        - id: route_auth
          uri: lb://auth
          predicates:
            - Path=/auth/**
          filters:
            - RewritePath=/auth/?(?<segment>.*), /$\{segment}
            - name: RequestSize
              args:
                maxSize: 5000000
            - name: RequestRateLimiter
              args:
                redis-rate-limiter:
                  replenishRate: 100
                  burstCapacity: 100
                  requestedTokens: 1
                key-resolver: "#{@myKeyResolver}"
      redis-rate-limiter:
        include-headers: false
      httpclient:
        connect-timeout: 10000
        response-timeout: 10s
  data:
    redis:
      time-to-live: 3600000
```

Delete `gateway/src/main/java/com/example/gateway/config/TokenConfig.java`.

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :gateway:test --tests 'com.example.gateway.config.GatewayRouteConfigTest'`  
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add gateway/build.gradle.kts gateway/src/main/java/com/example/gateway/GatewayApplication.java gateway/src/main/java/com/example/gateway/config/SecurityConfig.java gateway/src/main/resources/application.yml gateway/src/test/java/com/example/gateway/config/GatewayRouteConfigTest.java
git rm gateway/src/main/java/com/example/gateway/config/TokenConfig.java
git commit -m "feat: switch gateway to nacos-backed spring cloud gateway"
```

### Task 3: Rebuild `auth` as a Servlet Authorization Server Skeleton

**Files:**
- Modify: `auth/build.gradle.kts`, `auth/src/main/java/com/example/auth/AuthApplication.java`, `auth/src/main/java/com/example/auth/config/SecurityConfig.java`, `auth/src/main/resources/application.yml`
- Create: `auth/src/main/java/com/example/auth/config/AuthorizationServerConfig.java`, `auth/src/main/java/com/example/auth/config/PemKeyConfig.java`, `auth/src/test/java/com/example/auth/config/AuthorizationServerMetadataTest.java`, `auth/src/test/resources/application-test.yml`, `auth/src/main/resources/keys/jwt-private_pkcs8.pem`, `auth/src/main/resources/keys/jwt-public.pem`
- Delete: `auth/src/main/java/com/example/auth/config/RedisConfig.java`, `auth/src/main/java/com/example/auth/config/TokenConfig.java`, `gateway/src/main/resources/keys/jwt-private_pkcs8.pem`, `gateway/src/main/resources/keys/jwt-public.pem`

- [ ] **Step 1: Write the failing metadata test**

Create `auth/src/test/java/com/example/auth/config/AuthorizationServerMetadataTest.java`:

```java
package com.example.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.profiles.active=test",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.discovery.register-enabled=false"
})
@AutoConfigureMockMvc
class AuthorizationServerMetadataTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void openIdMetadataIsPublished() throws Exception {
        mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").value("http://127.0.0.1:9000"));
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :auth:test --tests 'com.example.auth.config.AuthorizationServerMetadataTest'`  
Expected: FAIL because the module still boots WebFlux security rather than a servlet authorization server.

- [ ] **Step 3: Write the minimal servlet authorization server implementation**

Write `auth/build.gradle.kts`:

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-authorization-server")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("com.h2database:h2")
}
```

Write `auth/src/main/java/com/example/auth/AuthApplication.java`:

```java
package com.example.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication(exclude = {org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration.class})
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }

}
```

Write `auth/src/main/java/com/example/auth/config/SecurityConfig.java`:

```java
package com.example.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import static org.springframework.http.MediaType.TEXT_HTML;

@Configuration
public class SecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                OAuth2AuthorizationServerConfigurer.authorizationServer();

        http.securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
                .with(authorizationServerConfigurer, authorizationServer ->
                        authorizationServer.oidc(Customizer.withDefaults()))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/login"),
                        new MediaTypeRequestMatcher(TEXT_HTML)))
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()));

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain appSecurityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/error").permitAll()
                        .anyRequest().authenticated())
                .formLogin(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

Write `auth/src/main/java/com/example/auth/config/AuthorizationServerConfig.java`:

```java
package com.example.auth.config;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

@Configuration
public class AuthorizationServerConfig {

    @Bean
    public RegisteredClientRepository registeredClientRepository(
            PasswordEncoder passwordEncoder,
            @Value("${app.client.redirect-uri}") String redirectUri
    ) {
        RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("client_id_1")
                .clientSecret(passwordEncoder.encode("client_secret_1"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri(redirectUri)
                .scope(OidcScopes.OPENID)
                .scope("profile")
                .scope("email")
                .build();

        return new InMemoryRegisteredClientRepository(client);
    }

    @Bean
    public OAuth2AuthorizationService authorizationService() {
        return new org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService();
    }

    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService() {
        return new org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationConsentService();
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings(@Value("${app.auth.issuer}") String issuer) {
        return AuthorizationServerSettings.builder()
                .issuer(issuer)
                .build();
    }
}
```

Write `auth/src/main/java/com/example/auth/config/PemKeyConfig.java`:

```java
package com.example.auth.config;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

@Configuration
public class PemKeyConfig {

    @Bean
    public JWKSource<SecurityContext> jwkSource() throws Exception {
        RSAPublicKey publicKey = readPublicKey("keys/jwt-public.pem");
        RSAPrivateKey privateKey = readPrivateKey("keys/jwt-private_pkcs8.pem");
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID("auth-server-key")
                .build();

        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    private RSAPublicKey readPublicKey(String path) throws Exception {
        String pem = readPem(path)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] decoded = Base64.getDecoder().decode(pem);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    private RSAPrivateKey readPrivateKey(String path) throws Exception {
        String pem = readPem(path)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] decoded = Base64.getDecoder().decode(pem);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    private String readPem(String path) throws Exception {
        byte[] bytes = FileCopyUtils.copyToByteArray(new ClassPathResource(path).getInputStream());
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
```

Write `auth/src/main/resources/application.yml`:

```yaml
# local
spring:
  config:
    activate:
      on-profile: local
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER_ADDR:127.0.0.1:8848}

server:
  port: 9000

app:
  auth:
    issuer: http://127.0.0.1:9000
  client:
    redirect-uri: http://127.0.0.1:8011/login/oauth2/code/my-registration

---
# dev
spring:
  config:
    activate:
      on-profile: dev
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER_ADDR}

server:
  port: ${AUTH_PORT}

app:
  auth:
    issuer: ${AUTH_ISSUER_URI}
  client:
    redirect-uri: ${CLIENT_REDIRECT_URI}

---
# common
spring:
  application:
    name: auth
  messages:
    basename: messages

server:
  shutdown: graceful
```

Write `auth/src/test/resources/application-test.yml`:

```yaml
app:
  auth:
    issuer: http://127.0.0.1:9000
  client:
    redirect-uri: http://127.0.0.1:8011/login/oauth2/code/my-registration
```

Move the existing example signing keys into the auth module:

```bash
mkdir -p auth/src/main/resources/keys
mv gateway/src/main/resources/keys/jwt-private_pkcs8.pem auth/src/main/resources/keys/jwt-private_pkcs8.pem
mv gateway/src/main/resources/keys/jwt-public.pem auth/src/main/resources/keys/jwt-public.pem
```

Delete `auth/src/main/java/com/example/auth/config/RedisConfig.java` and `auth/src/main/java/com/example/auth/config/TokenConfig.java`.

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :auth:test --tests 'com.example.auth.config.AuthorizationServerMetadataTest'`  
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add auth/build.gradle.kts auth/src/main/java/com/example/auth/AuthApplication.java auth/src/main/java/com/example/auth/config/SecurityConfig.java auth/src/main/java/com/example/auth/config/AuthorizationServerConfig.java auth/src/main/java/com/example/auth/config/PemKeyConfig.java auth/src/main/resources/application.yml auth/src/test/java/com/example/auth/config/AuthorizationServerMetadataTest.java auth/src/test/resources/application-test.yml auth/src/main/resources/keys/jwt-private_pkcs8.pem auth/src/main/resources/keys/jwt-public.pem
git rm auth/src/main/java/com/example/auth/config/RedisConfig.java auth/src/main/java/com/example/auth/config/TokenConfig.java gateway/src/main/resources/keys/jwt-private_pkcs8.pem gateway/src/main/resources/keys/jwt-public.pem
git commit -m "feat: rebuild auth as servlet authorization server"
```

### Task 4: Move Authorization Server Persistence to JDBC/PostgreSQL

**Files:**
- Modify: `auth/src/main/java/com/example/auth/config/AuthorizationServerConfig.java`, `auth/src/main/java/com/example/auth/config/SecurityConfig.java`, `auth/src/main/java/com/example/auth/service/JdbcUserDetailsService.java`, `auth/src/main/java/com/example/auth/repository/UserRepository.java`, `auth/src/main/resources/application.yml`, `auth/src/test/resources/application-test.yml`
- Create: `auth/src/test/java/com/example/auth/config/JdbcAuthorizationServerPersistenceTest.java`, `auth/src/main/resources/sql/oauth2-registered-client.sql`, `auth/src/main/resources/sql/oauth2-authorization.sql`, `auth/src/main/resources/sql/oauth2-authorization-consent.sql`, `auth/src/main/resources/sql/users.sql`
- Delete: every auth reactive persistence and serialization file listed in the file map above

- [ ] **Step 1: Write the failing JDBC persistence test**

Create `auth/src/test/java/com/example/auth/config/JdbcAuthorizationServerPersistenceTest.java`:

```java
package com.example.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.profiles.active=test",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.discovery.register-enabled=false"
})
class JdbcAuthorizationServerPersistenceTest {

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Autowired
    private UserDetailsService userDetailsService;

    @Test
    void seededClientAndUserAreLoadedFromJdbc() {
        assertThat(registeredClientRepository.findByClientId("client_id_1")).isNotNull();
        assertThat(userDetailsService.loadUserByUsername("user").getUsername()).isEqualTo("user");
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :auth:test --tests 'com.example.auth.config.JdbcAuthorizationServerPersistenceTest'`  
Expected: FAIL because the module still uses the temporary in-memory repositories.

- [ ] **Step 3: Replace the in-memory repositories with JDBC-backed persistence**

Write `auth/src/main/java/com/example/auth/config/AuthorizationServerConfig.java`:

```java
package com.example.auth.config;

import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;

import com.example.auth.domain.User;
import com.example.auth.repository.UserRepository;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

@Configuration
public class AuthorizationServerConfig {

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcRegisteredClientRepository(jdbcTemplate);
    }

    @Bean
    public OAuth2AuthorizationService authorizationService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository
    ) {
        return new org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService(
                jdbcTemplate,
                registeredClientRepository
        );
    }

    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository
    ) {
        return new org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService(
                jdbcTemplate,
                registeredClientRepository
        );
    }

    @Bean
    public ApplicationRunner seedRegisteredClient(
            RegisteredClientRepository registeredClientRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.client.redirect-uri}") String redirectUri
    ) {
        return args -> {
            if (registeredClientRepository.findByClientId("client_id_1") != null) {
                return;
            }

            RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId("client_id_1")
                    .clientSecret(passwordEncoder.encode("client_secret_1"))
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                    .redirectUri(redirectUri)
                    .scope(OidcScopes.OPENID)
                    .scope("profile")
                    .scope("email")
                    .build();

            registeredClientRepository.save(client);
        };
    }

    @Bean
    public ApplicationRunner seedUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findByUsername("user").isPresent()) {
                return;
            }

            userRepository.save(new User(
                    1L,
                    "user",
                    passwordEncoder.encode("1234"),
                    "user@example.com",
                    null,
                    null,
                    "ROLE_USER"
            ));
        };
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings(@Value("${app.auth.issuer}") String issuer) {
        return AuthorizationServerSettings.builder()
                .issuer(issuer)
                .build();
    }
}
```

Write `auth/src/main/java/com/example/auth/config/SecurityConfig.java`:

```java
package com.example.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import com.example.auth.repository.UserRepository;
import com.example.auth.service.JdbcUserDetailsService;

import static org.springframework.http.MediaType.TEXT_HTML;

@Configuration
public class SecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                OAuth2AuthorizationServerConfigurer.authorizationServer();

        http.securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
                .with(authorizationServerConfigurer, authorizationServer ->
                        authorizationServer.oidc(Customizer.withDefaults()))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/login"),
                        new MediaTypeRequestMatcher(TEXT_HTML)))
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()));

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain appSecurityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/error").permitAll()
                        .anyRequest().authenticated())
                .formLogin(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return new JdbcUserDetailsService(userRepository);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

Write `auth/src/main/java/com/example/auth/repository/UserRepository.java`:

```java
package com.example.auth.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import com.example.auth.domain.User;

public interface UserRepository extends CrudRepository<User, Long> {

    Optional<User> findByUsername(String username);

}
```

Write `auth/src/main/java/com/example/auth/service/JdbcUserDetailsService.java`:

```java
package com.example.auth.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.auth.repository.UserRepository;

@Service
public class JdbcUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public JdbcUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }
}
```

Write `auth/src/main/resources/application.yml`:

```yaml
# local
spring:
  config:
    activate:
      on-profile: local
  datasource:
    url: jdbc:postgresql://localhost:5432/my_db?currentSchema=my_schema
    username: postgres
    password: 1234
  sql:
    init:
      mode: always
      schema-locations:
        - classpath:sql/oauth2-registered-client.sql
        - classpath:sql/oauth2-authorization.sql
        - classpath:sql/oauth2-authorization-consent.sql
        - classpath:sql/users.sql
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER_ADDR:127.0.0.1:8848}

server:
  port: 9000

app:
  auth:
    issuer: http://127.0.0.1:9000
  client:
    redirect-uri: http://127.0.0.1:8011/login/oauth2/code/my-registration

---
# dev
spring:
  config:
    activate:
      on-profile: dev
  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/${POSTGRES_DB}?currentSchema=${POSTGRES_SCHEMA}
    username: ${POSTGRES_USER}
    password: ${POSTGRES_PASSWORD}
  sql:
    init:
      mode: always
      schema-locations:
        - classpath:sql/oauth2-registered-client.sql
        - classpath:sql/oauth2-authorization.sql
        - classpath:sql/oauth2-authorization-consent.sql
        - classpath:sql/users.sql
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER_ADDR}

server:
  port: ${AUTH_PORT}

app:
  auth:
    issuer: ${AUTH_ISSUER_URI}
  client:
    redirect-uri: ${CLIENT_REDIRECT_URI}

---
# common
spring:
  application:
    name: auth
  messages:
    basename: messages

server:
  shutdown: graceful
```

Write `auth/src/test/resources/application-test.yml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:authdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
  sql:
    init:
      mode: always
      schema-locations:
        - classpath:sql/oauth2-registered-client.sql
        - classpath:sql/oauth2-authorization.sql
        - classpath:sql/oauth2-authorization-consent.sql
        - classpath:sql/users.sql

app:
  auth:
    issuer: http://127.0.0.1:9000
  client:
    redirect-uri: http://127.0.0.1:8011/login/oauth2/code/my-registration
```

Write `auth/src/main/resources/sql/oauth2-registered-client.sql`:

```sql
create table if not exists oauth2_registered_client (
    id varchar(100) primary key,
    client_id varchar(100) not null unique,
    client_id_issued_at timestamp with time zone not null default current_timestamp,
    client_secret varchar(200) default null,
    client_secret_expires_at timestamp with time zone default null,
    client_name varchar(200) not null,
    client_authentication_methods varchar(1000) not null,
    authorization_grant_types varchar(1000) not null,
    redirect_uris varchar(1000) default null,
    post_logout_redirect_uris varchar(1000) default null,
    scopes varchar(1000) not null,
    client_settings varchar(2000) not null,
    token_settings varchar(2000) not null
);
```

Write `auth/src/main/resources/sql/oauth2-authorization.sql`:

```sql
create table if not exists oauth2_authorization (
    id varchar(100) primary key,
    registered_client_id varchar(100) not null,
    principal_name varchar(200) not null,
    authorization_grant_type varchar(100) not null,
    authorized_scopes varchar(1000) default null,
    attributes text default null,
    state varchar(500) default null,
    authorization_code_value bytea default null,
    authorization_code_issued_at timestamp with time zone default null,
    authorization_code_expires_at timestamp with time zone default null,
    authorization_code_metadata text default null,
    access_token_value bytea default null,
    access_token_issued_at timestamp with time zone default null,
    access_token_expires_at timestamp with time zone default null,
    access_token_metadata text default null,
    access_token_type varchar(100) default null,
    access_token_scopes varchar(1000) default null,
    oidc_id_token_value bytea default null,
    oidc_id_token_issued_at timestamp with time zone default null,
    oidc_id_token_expires_at timestamp with time zone default null,
    oidc_id_token_metadata text default null,
    refresh_token_value bytea default null,
    refresh_token_issued_at timestamp with time zone default null,
    refresh_token_expires_at timestamp with time zone default null,
    refresh_token_metadata text default null,
    user_code_value bytea default null,
    user_code_issued_at timestamp with time zone default null,
    user_code_expires_at timestamp with time zone default null,
    user_code_metadata text default null,
    device_code_value bytea default null,
    device_code_issued_at timestamp with time zone default null,
    device_code_expires_at timestamp with time zone default null,
    device_code_metadata text default null
);
```

Write `auth/src/main/resources/sql/oauth2-authorization-consent.sql`:

```sql
create table if not exists oauth2_authorization_consent (
    registered_client_id varchar(100) not null,
    principal_name varchar(200) not null,
    authorities varchar(1000) not null,
    primary key (registered_client_id, principal_name)
);
```

Write `auth/src/main/resources/sql/users.sql`:

```sql
create schema if not exists my_schema;

create table if not exists my_schema.users (
    id bigint primary key,
    username varchar(100) not null unique,
    password varchar(200) not null,
    email varchar(200) not null,
    created_at timestamp with time zone default current_timestamp,
    updated_at timestamp with time zone default current_timestamp,
    authorities varchar(500) not null
);
```

Delete the reactive persistence files from the file map with `git rm`.

- [ ] **Step 4: Run the targeted test to verify it passes**

Run: `./gradlew :auth:test --tests 'com.example.auth.config.JdbcAuthorizationServerPersistenceTest'`  
Expected: PASS.

- [ ] **Step 5: Run the auth test suite to keep the metadata test green**

Run: `./gradlew :auth:test`  
Expected: PASS for both `AuthorizationServerMetadataTest` and `JdbcAuthorizationServerPersistenceTest`.

- [ ] **Step 6: Commit**

```bash
git add auth/src/main/java/com/example/auth/config/AuthorizationServerConfig.java auth/src/main/java/com/example/auth/config/SecurityConfig.java auth/src/main/java/com/example/auth/service/JdbcUserDetailsService.java auth/src/main/java/com/example/auth/repository/UserRepository.java auth/src/main/resources/application.yml auth/src/test/java/com/example/auth/config/JdbcAuthorizationServerPersistenceTest.java auth/src/test/resources/application-test.yml auth/src/main/resources/sql
git rm auth/src/main/java/com/example/auth/domain/Client.java auth/src/main/java/com/example/auth/domain/OAuth2AuthorizationCodeGrantAuthorization.java auth/src/main/java/com/example/auth/domain/OAuth2AuthorizationGrantAuthorization.java auth/src/main/java/com/example/auth/domain/OAuth2ClientCredentialsGrantAuthorization.java auth/src/main/java/com/example/auth/domain/OAuth2UserConsent.java auth/src/main/java/com/example/auth/domain/OidcAuthorizationCodeGrantAuthorization.java auth/src/main/java/com/example/auth/domain/mixin/ClaimsHolderMixin.java auth/src/main/java/com/example/auth/domain/mixin/OAuth2AuthorizationCodeGrantAuthorizationMixin.java auth/src/main/java/com/example/auth/domain/mixin/OAuth2AuthorizationGrantAuthorizationMixin.java auth/src/main/java/com/example/auth/domain/mixin/OAuth2AuthorizationResponseTypeMixin.java auth/src/main/java/com/example/auth/repository/ClientRepository.java auth/src/main/java/com/example/auth/repository/JdbcRegisteredClientRepository.java auth/src/main/java/com/example/auth/repository/redis/OAuth2AuthorizationGrantAuthorizationRepository.java auth/src/main/java/com/example/auth/repository/redis/OAuth2UserConsentRepository.java auth/src/main/java/com/example/auth/service/RedisOAuth2AuthorizationConsentService.java auth/src/main/java/com/example/auth/service/RedisOAuth2AuthorizationService.java auth/src/main/java/com/example/auth/util/ModelMapper.java auth/src/main/java/com/example/auth/util/Serializer.java auth/src/main/java/com/example/auth/util/converter/BytesToClaimsHolderConverter.java auth/src/main/java/com/example/auth/util/converter/BytesToOAuth2AuthorizationRequestConverter.java auth/src/main/java/com/example/auth/util/converter/BytesToUsernamePasswordAuthenticationTokenConverter.java auth/src/main/java/com/example/auth/util/converter/ClaimsHolderToBytesConverter.java auth/src/main/java/com/example/auth/util/converter/OAuth2AuthorizationRequestToBytesConverter.java auth/src/main/java/com/example/auth/util/converter/UsernamePasswordAuthenticationTokenToBytesConverter.java
git commit -m "feat: move auth persistence to jdbc"
```

### Task 5: Convert `client` to a Servlet OAuth2 Client

**Files:**
- Modify: `client/build.gradle.kts`, `client/src/main/java/com/example/client/ClientApplication.java`, `client/src/main/java/com/example/client/config/SecurityConfig.java`, `client/src/main/java/com/example/client/controller/ClientController.java`, `client/src/main/resources/application.yml`, `client/src/main/resources/templates/index.html`
- Create: `client/src/main/resources/templates/profile.html`, `client/src/test/java/com/example/client/config/ClientSecurityConfigTest.java`
- Delete: `client/src/main/java/com/example/client/config/WebClientConfig.java`

- [ ] **Step 1: Write the failing client security test**

Create `client/src/test/java/com/example/client/config/ClientSecurityConfigTest.java`:

```java
package com.example.client.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(properties = {
        "spring.profiles.active=local",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.discovery.register-enabled=false"
})
@AutoConfigureMockMvc
class ClientSecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rootIsPublic() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    void profileRedirectsToOauthLoginWhenAnonymous() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/oauth2/authorization/my-registration"));
    }

    @Test
    void profileRendersForAuthenticatedOidcUser() throws Exception {
        mockMvc.perform(get("/profile")
                        .with(SecurityMockMvcRequestPostProcessors.oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"));
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :client:test --tests 'com.example.client.config.ClientSecurityConfigTest'`  
Expected: FAIL because the module still uses WebFlux security.

- [ ] **Step 3: Write the minimal servlet client implementation**

Write `client/build.gradle.kts`:

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
}
```

Write `client/src/main/java/com/example/client/ClientApplication.java`:

```java
package com.example.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class ClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }
}
```

Write `client/src/main/java/com/example/client/config/SecurityConfig.java`:

```java
package com.example.client.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/favicon.ico").permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(Customizer.withDefaults())
                .oauth2Client(Customizer.withDefaults())
                .logout(logout -> logout.logoutSuccessUrl("/"));

        return http.build();
    }
}
```

Write `client/src/main/java/com/example/client/controller/ClientController.java`:

```java
package com.example.client.controller;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ClientController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal OidcUser oidcUser, Model model) {
        model.addAttribute("name", oidcUser != null ? oidcUser.getSubject() : "anonymous");
        return "profile";
    }
}
```

Write `client/src/main/resources/application.yml`:

```yaml
# local
spring:
  config:
    activate:
      on-profile: local
  security:
    oauth2:
      client:
        registration:
          my-registration:
            provider: my-provider
            client-id: client_id_1
            client-secret: client_secret_1
            client-authentication-method: client_secret_post
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            scope: openid,profile,email
        provider:
          my-provider:
            issuer-uri: http://127.0.0.1:9000
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER_ADDR:127.0.0.1:8848}

server:
  port: 8011

---
# dev
spring:
  config:
    activate:
      on-profile: dev
  security:
    oauth2:
      client:
        registration:
          my-registration:
            provider: my-provider
            client-id: client_id_1
            client-secret: client_secret_1
            client-authentication-method: client_secret_post
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            scope: openid,profile,email
        provider:
          my-provider:
            issuer-uri: ${AUTH_ISSUER_URI}
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER_ADDR}

server:
  port: ${CLIENT_PORT}

---
# common
spring:
  application:
    name: client
```

Write `client/src/main/resources/templates/index.html`:

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Client</title>
</head>
<body>
<h1>Client</h1>
<p><a href="/profile">Open protected profile</a></p>
</body>
</html>
```

Write `client/src/main/resources/templates/profile.html`:

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Profile</title>
</head>
<body>
<h1>Profile</h1>
<p th:text="${name}">user</p>
</body>
</html>
```

Delete `client/src/main/java/com/example/client/config/WebClientConfig.java`.

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :client:test --tests 'com.example.client.config.ClientSecurityConfigTest'`  
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add client/build.gradle.kts client/src/main/java/com/example/client/ClientApplication.java client/src/main/java/com/example/client/config/SecurityConfig.java client/src/main/java/com/example/client/controller/ClientController.java client/src/main/resources/application.yml client/src/main/resources/templates/index.html client/src/main/resources/templates/profile.html client/src/test/java/com/example/client/config/ClientSecurityConfigTest.java
git rm client/src/main/java/com/example/client/config/WebClientConfig.java
git commit -m "feat: migrate client to servlet oauth2 login"
```

### Task 6: Remove `discovery` and Refresh Local Runtime Assets

**Files:**
- Modify: `settings.gradle.kts`, `docker-compose.yml`, `auth/Dockerfile`, `gateway/Dockerfile`, `client/Dockerfile`
- Create: `README.md`
- Delete: `discovery/build.gradle.kts`, `discovery/src/main/java/com/example/discovery/DiscoveryApplication.java`, `discovery/src/main/resources/application.yml`, `discovery/src/main/resources/logback-spring.xml`, `discovery/Dockerfile`

- [ ] **Step 1: Run the local runtime validation before the cleanup**

Run: `docker compose config`  
Expected: output still references the old `discovery` service and does not yet include `nacos`.

- [ ] **Step 2: Remove the discovery module from the build**

Write `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "spring-projects"

include("gateway")
include("auth")
include("client")
```

Delete the `discovery` source and resource files from the file map with `git rm`.

- [ ] **Step 3: Rewrite the local runtime assets for Nacos**

Write `docker-compose.yml`:

```yaml
version: "3.8"

services:
  nacos:
    image: nacos/nacos-server:v3.1.1
    container_name: nacos
    environment:
      MODE: standalone
      NACOS_AUTH_ENABLE: "false"
    ports:
      - "8848:8848"

  postgres:
    container_name: postgres
    image: postgres:16
    restart: always
    env_file:
      - .env
    ports:
      - "${POSTGRES_PORT}:${POSTGRES_PORT_DOCKER}"
    volumes:
      - ./postgres/data:/var/lib/postgresql/data
      - ./postgres/config/postgresql.conf:/etc/postgresql/postgresql.conf
      - ./postgres/init:/docker-entrypoint-initdb.d
    command: postgres -c 'config_file=/etc/postgresql/postgresql.conf'

  redis:
    container_name: redis
    image: redis:7
    restart: always
    env_file:
      - .env
    ports:
      - "${REDIS_PORT}:${REDIS_PORT_DOCKER}"

  auth:
    container_name: auth
    build:
      context: .
      dockerfile: auth/Dockerfile
    env_file:
      - .env
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - NACOS_SERVER_ADDR=nacos:8848
    ports:
      - "${AUTH_PORT}:${AUTH_PORT_DOCKER}"
    depends_on:
      - nacos
      - postgres

  gateway:
    container_name: gateway
    build:
      context: .
      dockerfile: gateway/Dockerfile
    env_file:
      - .env
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - NACOS_SERVER_ADDR=nacos:8848
    ports:
      - "${GATEWAY_PORT}:${GATEWAY_PORT_DOCKER}"
    depends_on:
      - nacos
      - auth
      - redis

  client:
    container_name: client
    build:
      context: .
      dockerfile: client/Dockerfile
    env_file:
      - .env
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - NACOS_SERVER_ADDR=nacos:8848
    ports:
      - "${CLIENT_PORT}:${CLIENT_PORT_DOCKER}"
    depends_on:
      - nacos
      - auth
```

Write `auth/Dockerfile`:

```dockerfile
FROM gradle:8.14.3-jdk21 AS builder
WORKDIR /workspace
COPY . .
RUN ./gradlew :auth:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /workspace/auth/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

Write `gateway/Dockerfile`:

```dockerfile
FROM gradle:8.14.3-jdk21 AS builder
WORKDIR /workspace
COPY . .
RUN ./gradlew :gateway:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /workspace/gateway/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

Write `client/Dockerfile`:

```dockerfile
FROM gradle:8.14.3-jdk21 AS builder
WORKDIR /workspace
COPY . .
RUN ./gradlew :client:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /workspace/client/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

Write `README.md`:

```md
# spring-examples

Spring Boot 4 sample workspace with:

- `auth`: servlet authorization server
- `gateway`: reactive API gateway
- `client`: servlet OAuth2 client
- `nacos`: service discovery for local runs

## Local infrastructure

~~~bash
docker compose up -d nacos postgres redis
~~~

## Local apps

~~~bash
./gradlew :auth:bootRun --args='--spring.profiles.active=local'
./gradlew :gateway:bootRun --args='--spring.profiles.active=local'
./gradlew :client:bootRun --args='--spring.profiles.active=local'
~~~

## Verification

~~~bash
./gradlew test
~~~
```

Delete `discovery/Dockerfile`.

- [ ] **Step 4: Run the final verification**

Run: `docker compose config && ./gradlew test`  
Expected: `docker compose config` shows `nacos`, `postgres`, `redis`, `auth`, `gateway`, and `client`, and `./gradlew test` ends with `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add settings.gradle.kts docker-compose.yml auth/Dockerfile gateway/Dockerfile client/Dockerfile README.md
git rm discovery/build.gradle.kts discovery/src/main/java/com/example/discovery/DiscoveryApplication.java discovery/src/main/resources/application.yml discovery/src/main/resources/logback-spring.xml discovery/Dockerfile
git commit -m "chore: remove discovery module and refresh local runtime docs"
```
