package com.spring.mcp.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers configuration for integration tests.
 * Provides a PostgreSQL container that supports array types, jsonb, and full-text search.
 * The pg_trgm extension is created via an init script that is executed before Flyway.
 */
@TestConfiguration
@Profile("test")
public class TestContainersConfig {

    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgresContainer() {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>(
                DockerImageName.parse("postgres:18-alpine"))
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test")
                // Create pg_trgm extension for fuzzy search support
                .withInitScript("db/init/01-init-extensions.sql");
        container.start();
        return container;
    }
}
