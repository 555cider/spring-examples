package com.example.auth.config;

import javax.sql.DataSource;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class PostgresUsersSchemaCompatibilityTest {

    private static final String USER_LOOKUP_SQL = """
            SELECT "my_schema"."users"."id" AS "id",
                   "my_schema"."users"."email" AS "email",
                   "my_schema"."users"."username" AS "username",
                   "my_schema"."users"."password" AS "password",
                   "my_schema"."users"."updated_at" AS "updated_at",
                   "my_schema"."users"."created_at" AS "created_at",
                   "my_schema"."users"."authorities" AS "authorities"
            FROM "my_schema"."users"
            WHERE "my_schema"."users"."username" = ?
            """;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        this.jdbcTemplate = new JdbcTemplate(dataSource());
        this.jdbcTemplate.execute("drop schema if exists my_schema cascade");
    }

    @Test
    void usersSqlCreatesPostgresColumnsThatJdbcQueriesCanRead() {
        new ResourceDatabasePopulator(new ClassPathResource("sql/users.sql")).execute(dataSource());

        jdbcTemplate.update("""
                insert into "my_schema"."users" (
                    "username",
                    "password",
                    "email",
                    "created_at",
                    "updated_at",
                    "authorities"
                ) values (?, ?, ?, current_timestamp, current_timestamp, ?)
                """,
                "user",
                "{noop}password",
                "user@example.com",
                "ROLE_USER"
        );

        String username = jdbcTemplate.queryForObject(
                USER_LOOKUP_SQL,
                (rs, rowNum) -> rs.getString("username"),
                "user"
        );

        assertThat(username).isEqualTo("user");
    }

    @Test
    void dockerComposeInitScriptSeedsUserPasswordAs1234() {
        new ResourceDatabasePopulator(dockerComposeInitScript()).execute(dataSource());

        String encodedPassword = jdbcTemplate.queryForObject(
                "select password from my_schema.users where username = ?",
                String.class,
                "user"
        );

        assertThat(new BCryptPasswordEncoder().matches("1234", encodedPassword)).isTrue();
    }

    private DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(postgres.getDriverClassName());
        dataSource.setUrl(postgres.getJdbcUrl());
        dataSource.setUsername(postgres.getUsername());
        dataSource.setPassword(postgres.getPassword());
        return dataSource;
    }

    private Resource dockerComposeInitScript() {
        Path scriptPath = Path.of("").toAbsolutePath()
                .resolve("../postgres/init/init.sql")
                .normalize();
        return new FileSystemResource(scriptPath);
    }
}
