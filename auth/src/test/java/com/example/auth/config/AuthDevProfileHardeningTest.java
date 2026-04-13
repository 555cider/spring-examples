package com.example.auth.config;

import com.example.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.profiles.active=dev",
        "spring.datasource.url=jdbc:tc:postgresql:16:///authdb?currentSchema=my_schema",
        "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",
        "spring.datasource.username=test",
        "spring.datasource.password=test",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:sql/oauth2-registered-client.sql,classpath:sql/oauth2-authorization.sql,classpath:sql/oauth2-authorization-consent.sql,classpath:sql/users.sql",
        "server.port=0",
        "app.auth.issuer=http://localhost:8080/auth",
        "app.client.redirect-uri=http://127.0.0.1:8011/login/oauth2/code/my-registration",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.discovery.register-enabled=false"
})
class AuthDevProfileHardeningTest {

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void devProfileDoesNotSeedDemoClientOrUsersByDefault() {
        assertThat(registeredClientRepository.findByClientId("client_id_1")).isNull();
        assertThat(userRepository.findByUsername("admin")).isEmpty();
        assertThat(userRepository.findByUsername("user")).isEmpty();
    }
}
