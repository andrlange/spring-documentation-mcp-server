package com.spring.mcp.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for MCP Server
 * Provides health check information for Spring Boot Actuator
 */
@Component
public class McpHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        try {
            // MCP Server is autoconfigured by Spring AI
            // If this bean exists, the server is operational
            return Health.up()
                .withDetail("endpoint", "/mcp/spring")
                .withDetail("protocol", "Streamable-HTTP (MCP 2025-11-25)")
                .withDetail("springAiVersion", "1.1.2")
                .withDetail("serverName", "Spring Documentation MCP Server")
                .withDetail("status", "operational")
                .withDetail("autoConfigured", true)
                .build();
        } catch (Exception e) {
            return Health.down(e)
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
